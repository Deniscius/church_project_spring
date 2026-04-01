package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID publicId,
        String nom,
        String prenom,
        String fullName,
        String username,
        UserRole role,
        Boolean isGlobal,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}