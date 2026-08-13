package com.eyram.dev.church_project_spring.DTO.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Horaires publics d'une paroisse à abonnement actif (accueil fidèles).
 * Les créneaux correspondent au programme résolu de la semaine courante
 * (hebdomadaires + ponctuels / messe unique).
 */
public record ParoisseHorairesPublicResponse(
        UUID paroissePublicId,
        String paroisseNom,
        String doyenneNom,
        List<Creneau> horaires
) {
    public record Creneau(
            UUID publicId,
            /** Date calendaire du créneau dans la semaine affichée (Africa/Lomé). */
            LocalDate date,
            String jourSemaine,
            String jourLibelle,
            LocalTime heureCelebration,
            String libelle,
            /** Honoraire (NORMALE / DOMINICALE / SPECIALE) pour préremplir la formule. */
            String natureHonoraire,
            boolean dateSpecifique,
            boolean uniqueSurParoisse
    ) {
    }
}
