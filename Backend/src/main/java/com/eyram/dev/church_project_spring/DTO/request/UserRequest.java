package com.eyram.dev.church_project_spring.DTO.request;

import java.util.List;

import com.eyram.dev.church_project_spring.enums.UserRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRequest(

        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
        String nom,

        @NotBlank(message = "Le prénom est obligatoire")
        @Size(max = 150, message = "Le prénom ne doit pas dépasser 150 caractères")
        String prenom,

        @NotBlank(message = "Le nom d'utilisateur est obligatoire")
        @Size(max = 100, message = "Le nom d'utilisateur ne doit pas dépasser 100 caractères")
        String username,

        @NotBlank(message = "Le mot de passe est obligatoire")
        String password,

        @NotNull(message = "Le statut global est obligatoire")
        Boolean isGlobal,

        @NotNull(message = "Le statut actif est obligatoire")
        Boolean isActive,

        UserRole role,

        List<ParoisseAssignmentRequest> paroisses

) {
}