package com.eyram.dev.church_project_spring.DTO.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record TypeDemandeResponse(
        UUID publicId,
        String libelle,
        String description,
        Boolean isActive,
        UUID paroissePublicId,
        String paroisseNom,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}