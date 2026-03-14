package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.RoleParoisse;

import java.time.LocalDateTime;
import java.util.UUID;

public record ParoisseAccessResponse(
        UUID publicId,
        UUID userPublicId,
        String userNom,
        String userPrenom,
        String username,
        UUID paroissePublicId,
        String paroisseNom,
        RoleParoisse roleParoisse,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}