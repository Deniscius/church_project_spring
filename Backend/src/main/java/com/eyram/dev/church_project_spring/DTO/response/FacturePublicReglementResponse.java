package com.eyram.dev.church_project_spring.DTO.response;

import java.time.LocalDateTime;

/** Détail d'encaissement public — sans idTransaction / numéro payeur. */
public record FacturePublicReglementResponse(
        LocalDateTime datePaiement,
        String statut,
        Integer montantCharge,
        Integer montantFrais,
        Integer montantFraisAgregateur,
        Integer montantFraisPlateforme,
        Integer montantNet,
        String provider
) {
}
