package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;

import java.time.LocalDateTime;

/**
 * Règlement effectif d'une facture, tel que renvoyé par l'agrégateur.
 * <p>
 * {@code montantCharge} est ce que le fidèle a réellement payé,
 * {@code montantNet} ce qui revient à la paroisse une fois les frais déduits :
 * la différence explique l'écart entre le montant facturé et le solde de
 * trésorerie.
 */
public record PaiementReglementResponse(
        LocalDateTime datePaiement,
        StatutPaiementEnum statut,
        Integer montantCharge,
        Integer montantFrais,
        Integer montantFraisAgregateur,
        Integer montantFraisPlateforme,
        Integer montantNet,
        String provider,
        String idTransaction,
        String numeroPayeur
) {
}
