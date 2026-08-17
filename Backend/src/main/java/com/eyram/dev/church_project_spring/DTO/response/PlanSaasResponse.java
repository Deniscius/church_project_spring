package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.PlanAbonnement;

import java.time.LocalDateTime;
import java.util.UUID;

public record PlanSaasResponse(
        UUID publicId,
        PlanAbonnement code,
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
