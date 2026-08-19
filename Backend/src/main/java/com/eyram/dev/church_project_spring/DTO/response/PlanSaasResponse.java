package com.eyram.dev.church_project_spring.DTO.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record PlanSaasResponse(
        UUID publicId,
        String code,
        String nom,
        String description,
        Integer montantXof,
        Integer dureeMois,
        Boolean actif,
        Boolean featured,
        Integer ordreAffichage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
