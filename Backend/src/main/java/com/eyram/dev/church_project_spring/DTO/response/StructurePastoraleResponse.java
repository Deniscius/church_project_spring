package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.TypeStructurePastorale;

import java.time.LocalDateTime;
import java.util.UUID;

public record StructurePastoraleResponse(
        UUID publicId,
        TypeStructurePastorale type,
        String nom,
        String attributions,
        Integer ordreAffichage,
        Boolean actif,
        Long version,
        UUID paroissePublicId,
        String paroisseNom,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
