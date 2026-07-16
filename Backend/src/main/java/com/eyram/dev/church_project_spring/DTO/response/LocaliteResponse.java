package com.eyram.dev.church_project_spring.DTO.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record LocaliteResponse(
        UUID publicId,
        String ville,
        String quartier,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
