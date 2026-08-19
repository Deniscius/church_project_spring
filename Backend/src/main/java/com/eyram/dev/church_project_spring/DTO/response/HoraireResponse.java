package com.eyram.dev.church_project_spring.DTO.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record HoraireResponse(
        UUID publicId,
        String jourSemaine,
        LocalTime heureCelebration,
        String libelle,
        Boolean isActive,
        LocalDate dateSpecifique,
        Boolean uniqueSurParoisse,
        Boolean programmeJourOverride,
        String natureHonoraire,
        UUID paroissePublicId,
        String paroisseNom,
        Boolean statusDel,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
