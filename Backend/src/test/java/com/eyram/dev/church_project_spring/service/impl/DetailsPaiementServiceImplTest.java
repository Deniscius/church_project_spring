package com.eyram.dev.church_project_spring.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;
import com.eyram.dev.church_project_spring.enums.StatutValidationEnum;
import com.eyram.dev.church_project_spring.mappers.DetailsPaiementMapper;
import com.eyram.dev.church_project_spring.repositories.DemandeRepository;
import com.eyram.dev.church_project_spring.repositories.DetailsPaiementRepository;
import com.eyram.dev.church_project_spring.repositories.FactureRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.repositories.TypePaiementRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.service.DemandePaymentEligibilityService;
import com.eyram.dev.church_project_spring.service.accounting.ParishLedgerService;
import com.eyram.dev.church_project_spring.service.payment.FedaPayPaymentService;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DetailsPaiementServiceImplTest {

    @Mock private DetailsPaiementRepository detailsPaiementRepository;
    @Mock private TypePaiementRepository typePaiementRepository;
    @Mock private FactureRepository factureRepository;
    @Mock private DemandeRepository demandeRepository;
    @Mock private ParoisseRepository paroisseRepository;
    @Mock private DetailsPaiementMapper detailsPaiementMapper;
    @Mock private TenantAccessService tenantAccessService;
    @Mock private FedaPayPaymentService fedaPayPaymentService;
    @Mock private ParishLedgerService parishLedgerService;
    @Mock private DemandePaymentEligibilityService demandePaymentEligibilityService;

    @Test
    void encaisserCaisseRejectsDemandAwaitingValidationBeforePaymentLookup() {
        UUID demandeId = UUID.randomUUID();
        Paroisse parish = new Paroisse();
        parish.setPublicId(UUID.randomUUID());

        Demande demande = new Demande();
        demande.setPublicId(demandeId);
        demande.setParoisse(parish);
        demande.setStatutValidation(StatutValidationEnum.EN_ATTENTE);
        demande.setStatutPaiement(StatutPaiementEnum.NON_PAYE);

        when(demandeRepository.findByPublicIdAndStatusDelFalse(demandeId))
                .thenReturn(Optional.of(demande));
        doThrow(new BusinessRuleException("Validation requise"))
                .when(demandePaymentEligibilityService).assertCanStartPayment(demande);

        DetailsPaiementServiceImpl service = new DetailsPaiementServiceImpl(
                detailsPaiementRepository,
                typePaiementRepository,
                factureRepository,
                demandeRepository,
                paroisseRepository,
                detailsPaiementMapper,
                tenantAccessService,
                fedaPayPaymentService,
                parishLedgerService,
                demandePaymentEligibilityService
        );

        assertThrows(BusinessRuleException.class, () -> service.encaisserCaisse(demandeId));
        verify(tenantAccessService).checkParoisseAccess(parish);
        verify(demandePaymentEligibilityService).assertCanStartPayment(demande);
        verify(typePaiementRepository, never()).findByModeAndStatusDelFalse(org.mockito.ArgumentMatchers.any());
        verify(demandeRepository, never()).save(demande);
    }
}
