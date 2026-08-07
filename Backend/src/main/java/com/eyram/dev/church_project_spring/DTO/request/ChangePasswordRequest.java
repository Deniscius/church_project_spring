package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Changement de mot de passe en libre-service : la connaissance du mot de passe
 * courant est exigée, personne d'autre ne peut effectuer l'opération.
 */
public record ChangePasswordRequest(

        @NotBlank(message = "Le mot de passe actuel est obligatoire")
        String currentPassword,

        @NotBlank(message = "Le nouveau mot de passe est obligatoire")
        @Size(min = 8, max = 200, message = "Le mot de passe doit contenir entre 8 et 200 caractères")
        String newPassword
) {
}
