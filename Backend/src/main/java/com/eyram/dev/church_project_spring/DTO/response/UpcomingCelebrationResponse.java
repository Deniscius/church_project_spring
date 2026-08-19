package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.StatutDemandeEnum;
import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Une célébration concrètement programmée pour une demande de messe.
 *
 * Contrairement au programme des horaires (créneaux disponibles), cette vue
 * représente les intentions déjà enregistrées sur des dates à venir.
 */
public record UpcomingCelebrationResponse(
        UUID demandePublicId,
        UUID demandeDatePublicId,
        String codeSuivie,
        LocalDate dateCelebration,
        LocalTime heureCelebration,
        String horaireLibelle,
        String intention,
        String typeDemandeLibelle,
        String fidele,
        StatutDemandeEnum statutDemande,
        StatutPaiementEnum statutPaiement
) {
}
