package com.eyram.dev.church_project_spring.DTO.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record MandatPastoralResponse(
        UUID publicId,
        String fonction,
        String attributions,
        LocalDate dateDebut,
        LocalDate dateFin,
        Integer ordreAffichage,
        Long version,
        UUID paroissePublicId,
        UUID anneePastoralePublicId,
        String anneePastoraleLibelle,
        UUID acteurPastoralPublicId,
        String acteurNom,
        String acteurPrenoms,
        String acteurAppellation,
        String acteurTelephone,
        String acteurEmail,
        UUID structurePastoralePublicId,
        String structurePastoraleNom,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
