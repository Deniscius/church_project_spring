package com.eyram.dev.church_project_spring.DTO.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChoraleResponse(
        UUID publicId,
        String nom,
        String description,
        Boolean isActive,
        UUID paroissePublicId,
        String paroisseNom,
        UUID doyennePublicId,
        String doyenneNom,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
