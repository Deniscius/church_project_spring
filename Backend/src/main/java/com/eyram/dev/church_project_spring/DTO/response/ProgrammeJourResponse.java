package com.eyram.dev.church_project_spring.DTO.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Programme résolu pour une date : créneaux hebdomadaires + ponctuels,
 * en tenant compte d'une éventuelle messe unique sur la paroisse.
 */
public record ProgrammeJourResponse(
        LocalDate date,
        String jourSemaine,
        String jourLibelle,
        boolean messeUnique,
        List<Creneau> creneaux
) {
    public record Creneau(
            UUID horairePublicId,
            LocalTime heureCelebration,
            String libelle,
            boolean dateSpecifique,
            boolean uniqueSurParoisse,
            String natureHonoraire
    ) {
    }
}
