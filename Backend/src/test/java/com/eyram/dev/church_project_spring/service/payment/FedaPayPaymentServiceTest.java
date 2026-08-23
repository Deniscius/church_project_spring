package com.eyram.dev.church_project_spring.service.payment;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.eyram.dev.church_project_spring.config.FedaPayProperties;
import com.eyram.dev.church_project_spring.entities.DetailsPaiement;
import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;
import com.eyram.dev.church_project_spring.repositories.DemandeRepository;
import com.eyram.dev.church_project_spring.repositories.DetailsPaiementRepository;
import com.eyram.dev.church_project_spring.repositories.FactureRepository;
import com.eyram.dev.church_project_spring.service.accounting.ParishLedgerService;
import com.eyram.dev.church_project_spring.service.billing.SubscriptionBillingService;
import com.eyram.dev.church_project_spring.service.payment.fedapay.FedaPayClient;
import com.eyram.dev.church_project_spring.service.payment.fedapay.FedaPayWebhookVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class FedaPayPaymentServiceTest {

    @Mock private DemandeRepository demandeRepository;
    @Mock private FactureRepository factureRepository;
    @Mock private DetailsPaiementRepository detailsPaiementRepository;
    @Mock private PaymentFeeCalculator feeCalculator;
    @Mock private PaymentReturnTokenService paymentReturnTokenService;
    @Mock private FedaPayClient fedaPayClient;
    @Mock private FedaPayWebhookVerifier webhookVerifier;
    @Mock private ParishLedgerService parishLedgerService;
    @Mock private SubscriptionBillingService subscriptionBillingService;
    @Mock private TransactionTemplate transactionTemplate;

    @Test
    void replayedApprovedWebhookDoesNotPersistOrCreditAgain() {
        String payload =
                "{\"name\":\"transaction.approved\",\"entity\":{\"id\":123,\"status\":\"approved\"}}";
        String signature = "t=1,s=test";

        DetailsPaiement paid = new DetailsPaiement();
        paid.setIdTransaction("123");
        paid.setStatutPaiement(StatutPaiementEnum.PAYE);
        paid.setStatusDel(false);
        when(detailsPaiementRepository.findByIdTransactionAndStatusDelFalse("123"))
                .thenReturn(Optional.of(paid));

        FedaPayPaymentService service = new FedaPayPaymentService(
                demandeRepository,
                factureRepository,
                detailsPaiementRepository,
                feeCalculator,
                paymentReturnTokenService,
                fedaPayClient,
                webhookVerifier,
                new FedaPayProperties(),
                new ObjectMapper(),
                parishLedgerService,
                subscriptionBillingService,
                transactionTemplate
        );

        service.handleWebhook(payload, signature);
        service.handleWebhook(payload, signature);

        verify(webhookVerifier, times(2)).verify(payload, signature);
        verify(detailsPaiementRepository, never()).save(paid);
        verify(parishLedgerService, never()).creditMesse(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
        verify(subscriptionBillingService, never())
                .activateFromProviderTransaction(org.mockito.ArgumentMatchers.any());
    }
}
