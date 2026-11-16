package com.eyram.dev.church_project_spring.DTO.response;

import java.time.LocalDateTime;

/** Détail d'encaissement public — sans idTransaction / numéro payeur. */
public record FacturePublicReglementResponse(
        LocalDateTime datePaiement,
        String statut,
        java.math.BigDecimal montantCharge,
        java.math.BigDecimal montantFrais,
        java.math.BigDecimal montantFraisAgregateur,
        Integer montantFraisPlateforme,
        java.math.BigDecimal montantNet,
        String provider
) {
}
