package com.eyram.dev.church_project_spring.DTO.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Une intention à célébrer un jour donné (ligne de la feuille sacristain).
 * {@code dateCelebration} = jour de la messe ; {@code dateDepot} = création de la demande.
 */
public record CelebrationIntentionResponse(
        UUID demandeDatePublicId,
        UUID demandePublicId,
        String codeSuivie,
        LocalDate dateCelebration,
        LocalDateTime dateDepot,
        Integer ordre,
        Integer nombreCelebration,
        String dureeLabel,
        String progressionLabel,
        LocalTime heure,
        String horaireLibelle,
        String intention,
        String typeDemandeLibelle,
        String forfaitNom,
        String natureForfait,
        String demandeurNom,
        String demandeurPrenom,
        String demandeurTelephone,
        String demandeurEmail,
        String statutDemande,
        String statutPaiement,
        boolean celebre,
        LocalDateTime celebreAt
) {
}
