package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.StatutDemandeEnum;
import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Une célébration concrètement programmée pour une demande de messe.
 *
 * Contrairement au programme des horaires (créneaux disponibles), cette vue
 * représente une intention réellement rattachée à une date de célébration.
 */
public record UpcomingCelebrationResponse(
        UUID demandePublicId,
        UUID demandeDatePublicId,
        String codeSuivie,
        LocalDate dateCelebration,
        LocalTime heureCelebration,
        UUID horairePublicId,
        String horaireLibelle,
        String intention,
        String typeDemandeLibelle,
        String fidele,
        StatutDemandeEnum statutDemande,
        StatutPaiementEnum statutPaiement,
        boolean celebre,
        LocalDateTime celebreAt,
        boolean modifiable,
        boolean disponible,
        String indisponibiliteMotif
) {
}
