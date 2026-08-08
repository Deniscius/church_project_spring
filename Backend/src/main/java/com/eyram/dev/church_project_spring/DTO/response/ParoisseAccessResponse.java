package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.RoleParoisse;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Rattachement d'un utilisateur à une paroisse, enrichi des coordonnées des
 * deux côtés : la plateforme doit pouvoir joindre un responsable sans passer
 * par plusieurs écrans.
 */
public record ParoisseAccessResponse(
        UUID publicId,

        UUID userPublicId,
        String userNom,
        String userPrenom,
        String username,
        String userEmail,
        String userTelephone,
        String userRole,
        Boolean userActive,

        UUID paroissePublicId,
        String paroisseNom,
        String paroisseEmail,
        String paroisseTelephone,
        String doyenneNom,
        Boolean paroisseActive,
        LocalDateTime paroisseSubscriptionExpiresAt,

        RoleParoisse roleParoisse,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}