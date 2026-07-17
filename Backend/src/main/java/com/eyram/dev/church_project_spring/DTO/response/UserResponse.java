package com.eyram.dev.church_project_spring.DTO.response;

import java.util.UUID;

/**
 * DTO de réponse utilisateur exposé aux interfaces d'administration.
 */
public record UserResponse(
        UUID publicId,
        String nom,
        String prenom,
        String username,
        String role,
        Boolean isActive,
        Boolean isGlobal
) {
}
