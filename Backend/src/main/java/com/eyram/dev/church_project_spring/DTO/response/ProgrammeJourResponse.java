package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.ModeProgrammeJour;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Programme résolu pour une date : grille hebdomadaire, ajouts ponctuels ou
 * personnalisation complète de la journée.
 */
public record ProgrammeJourResponse(
        LocalDate date,
        String jourSemaine,
        String jourLibelle,
        ModeProgrammeJour modeProgramme,
        boolean messeUnique,
        List<Creneau> creneaux
) {
    public record Creneau(
            UUID horairePublicId,
            LocalTime heureCelebration,
            String libelle,
            boolean dateSpecifique,
            boolean uniqueSurParoisse,
            boolean programmeJourOverride,
            String natureHonoraire
    ) {
    }
}
