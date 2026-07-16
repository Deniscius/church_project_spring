package com.eyram.dev.church_project_spring.DTO.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ParoisseResponse(
        UUID publicId,
        String nom,
        String adresse,
        String email,
        String telephone,
        Boolean isActive,
        UUID localitePublicId,
        String localiteVille,
        String localiteQuartier,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
