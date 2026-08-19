package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.StatutDemandeEnum;
import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Résumé public minimal d'une demande retrouvée par téléphone.
 * Ne contient ni intention, ni identité du fidèle, ni autre donnée personnelle.
 */
public record TrackingByPhoneItemResponse(
        String codeSuivie,
        LocalDateTime createdAt,
        String paroisseNom,
        String typeDemandeLibelle,
        StatutDemandeEnum statutDemande,
        StatutPaiementEnum statutPaiement,
        List<LocalDate> datesCelebration
) {
}
