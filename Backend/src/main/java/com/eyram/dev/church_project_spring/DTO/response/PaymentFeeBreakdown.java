package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.FeePayer;
import com.eyram.dev.church_project_spring.enums.ModePaiement;

import java.math.BigDecimal;

/**
 * Répartition d'un paiement messe :
 * - montantFacture / montantNetParoisse : part fixe paroisse (prix messe)
 * - montantFraisPlateforme : part développeur / plateforme
 * - montantFraisAgregeateur : part du prestataire
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
    @com.fasterxml.jackson.annotation.JsonProperty("providerFeesPending")
    public boolean providerFeesPending(){return feePercentAgregeateur==null;}

    /** Compat : ancien champ montantNet = net paroisse. */
    public int montantNet() {
        return montantNetParoisse;
    }
}
