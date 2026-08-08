package com.eyram.dev.church_project_spring.DTO.request;

import java.util.List;

import com.eyram.dev.church_project_spring.enums.UserRole;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserRequest(

        @NotBlank(message = "Le nom est obligatoire")
        @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
        String nom,

        @NotBlank(message = "Le prénom est obligatoire")
        @Size(min = 2, max = 150, message = "Le prénom doit contenir entre 2 et 150 caractères")
        String prenom,

        @NotBlank(message = "Le nom d'utilisateur est obligatoire")
        @Size(min = 3, max = 100, message = "Le nom d'utilisateur doit contenir entre 3 et 100 caractères")
        @Pattern(
                regexp = "^[A-Za-z0-9._-]+$",
                message = "Le nom d'utilisateur contient des caractères non autorisés"
        )
        String username,

        @Email(message = "L'adresse e-mail est invalide")
        @Size(max = 150, message = "L'adresse e-mail ne doit pas dépasser 150 caractères")
        String email,

        @Size(max = 50, message = "Le téléphone ne doit pas dépasser 50 caractères")
        String telephone,

        @NotBlank(message = "Le mot de passe est obligatoire", groups = Create.class)
        @Pattern(
                regexp = "^$|.{8,}$",
                message = "Le mot de passe doit contenir au minimum 8 caractères"
        )
        @Size(max = 200, message = "Le mot de passe ne doit pas dépasser 200 caractères")
        String password,

        @NotNull(message = "Le statut global est obligatoire")
        Boolean isGlobal,

        @NotNull(message = "Le statut actif est obligatoire")
        Boolean isActive,

        @NotNull(message = "Le rôle est obligatoire")
        UserRole role,

        List<@Valid ParoisseAssignmentRequest> paroisses

) {
    public interface Create {
    }
}
