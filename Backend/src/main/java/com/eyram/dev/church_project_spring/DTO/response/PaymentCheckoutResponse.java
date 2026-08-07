package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.ModePaiement;
import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;

import java.math.BigDecimal;

public record PaymentCheckoutResponse(
        String codeSuivie,
        StatutPaiementEnum statutPaiement,
        ModePaiement modePaiement,
        boolean onlinePaymentRequired,
        String paymentUrl,
        String providerTransactionId,
        int montantFacture,
        int montantFraisAgregeateur,
        int montantFraisPlateforme,
        int montantFrais,
        int montantCharge,
        int montantNetParoisse,
        int montantNetPlateforme,
        BigDecimal feePercentAgregeateur,
        BigDecimal feePercentPlateforme,
        String feePayer,
        String message
) {
}
