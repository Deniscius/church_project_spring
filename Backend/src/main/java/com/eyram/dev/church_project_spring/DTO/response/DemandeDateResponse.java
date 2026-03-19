package com.eyram.dev.church_project_spring.DTO.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record DemandeDateResponse(
        UUID publicId,
        Integer ordre,
        LocalDate dateCelebration,
        UUID demandePublicId,
        String codeSuivieDemande,
        Boolean statusDel,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}