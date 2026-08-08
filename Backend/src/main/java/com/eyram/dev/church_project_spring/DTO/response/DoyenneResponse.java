package com.eyram.dev.church_project_spring.DTO.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record DoyenneResponse(
        UUID publicId,
        String nom,
        String description,
        Integer rang,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
