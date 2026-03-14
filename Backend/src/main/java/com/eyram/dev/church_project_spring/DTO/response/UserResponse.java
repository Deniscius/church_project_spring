package com.eyram.dev.church_project_spring.DTO.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID publicId,
        String nom,
        String prenom,
        String fullName,
        String username,
        Boolean isGlobal,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}