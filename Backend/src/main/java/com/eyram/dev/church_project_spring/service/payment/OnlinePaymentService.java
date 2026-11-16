package com.eyram.dev.church_project_spring.service.payment;

import com.eyram.dev.church_project_spring.DTO.response.*;
import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;
import com.eyram.dev.church_project_spring.repositories.DetailsPaiementRepository;
import com.eyram.dev.church_project_spring.repositories.FactureRepository;
import com.eyram.dev.church_project_spring.service.payment.saspay.SasPayPaymentService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

/** SasPay for new checkouts; existing FedaPay transactions remain reconciliable. */
@Service
@RequiredArgsConstructor
public class OnlinePaymentService {
    private final SasPayPaymentService saspay;
    private final FedaPayPaymentService legacy;
    private final PaymentReturnTokenService tokens;
    private final FactureRepository invoices;
    private final DetailsPaiementRepository payments;
    private final TransactionTemplate transactions;

    private boolean legacy(String code) {
        return Boolean.TRUE.equals(
                transactions.execute(
                        t ->
                                invoices.findByDemandeCodeSuivieAndStatusDelFalse(code)
                                        .flatMap(
                                                i ->
                                                        payments
                                                                .findByFacturePublicIdAndStatusDelFalse(
                                                                        i.getPublicId()))
                                        .map(
                                                d ->
                                                        "FEDAPAY".equals(d.getProvider())
                                                                && d.getIdTransaction() != null)
                                        .orElse(false)));
    }

    public PaymentFeeBreakdown quoteByTrackingCode(String code) {
        return legacy(code) ? legacy.quoteByTrackingCode(code) : saspay.quote(code);
    }

    public PaymentCheckoutResponse checkout(String code, String email) {
        if (legacy(code)) {
            var existing = legacy.reconcileByTrackingCode(code, null);
            if (existing.statutPaiement() == StatutPaiementEnum.PAYE
                    || existing.statutPaiement() == StatutPaiementEnum.EN_ATTENTE) return existing;
        }
        return saspay.checkout(code, email);
    }

    public PaymentCheckoutResponse reconcileByTrackingCode(String code, String hint) {
        return legacy(code) ? legacy.reconcileByTrackingCode(code, hint) : saspay.reconcile(code);
    }

    public Map<String, String> resolveReturnAndReconcile(String token, String hint) {
        String code = tokens.resolve(token);
        var status = reconcileByTrackingCode(code, legacy(code) ? hint : null);
        return Map.of("codeSuivie", code, "statutPaiement", status.statutPaiement().name());
    }
}
