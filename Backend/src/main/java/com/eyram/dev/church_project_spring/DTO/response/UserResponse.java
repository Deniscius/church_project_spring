package com.eyram.dev.church_project_spring.DTO.response;

import java.util.List;
import java.util.UUID;

import com.eyram.dev.church_project_spring.entities.ParoisseAccess;

/**
 * DTO pour la réponse utilisateur.
 * Utilisé pour retourner les informations d'un utilisateur avec ses accès paroissiaux.
 */
public record UserResponse(
        UUID publicId,
        String nom,
        String prenom,
        String username,
        String role,
        List<ParoisseAccess> paroisses
) {
}