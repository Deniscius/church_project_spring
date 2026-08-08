package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.FeePayer;
import com.eyram.dev.church_project_spring.enums.ModePaiement;

import java.math.BigDecimal;

/**
 * Répartition d'un paiement messe :
 * - montantFacture / montantNetParoisse : part fixe paroisse (prix messe)
 * - montantFraisPlateforme : part développeur / plateforme
 * - montantFraisAgregeateur : part FedaPay
 * - montantCharge : total débité au fidèle (compte marchand)
 */
public record PaymentFeeBreakdown(
        ModePaiement mode,
        FeePayer feePayer,
        BigDecimal feePercentAgregeateur,
        BigDecimal feePercentPlateforme,
        int fixedFeeAgregeateurXof,
        int fixedFeePlateformeXof,
        int montantFacture,
        int montantFraisAgregeateur,
        int montantFraisPlateforme,
        int montantFrais,
        int montantCharge,
        int montantNetParoisse,
        int montantNetPlateforme
) {
    /** Compat : ancien champ montantNet = net paroisse. */
    public int montantNet() {
        return montantNetParoisse;
    }
}
