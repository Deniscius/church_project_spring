package com.eyram.dev.church_project_spring.DTO.response;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Horaires publics d'une paroisse à abonnement actif (accueil fidèles).
 */
public record ParoisseHorairesPublicResponse(
        UUID paroissePublicId,
        String paroisseNom,
        String doyenneNom,
        List<Creneau> horaires
) {
    public record Creneau(
            UUID publicId,
            String jourSemaine,
            String jourLibelle,
            LocalTime heureCelebration,
            String libelle,
            /** Honoraire (NORMALE / DOMINICALE / SPECIALE) pour préremplir la formule. */
            String natureHonoraire
    ) {
    }
}
