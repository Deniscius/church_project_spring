package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Informations personnelles modifiables par l'utilisateur connecté lui-même.
 * Le nom d'utilisateur, le rôle et le périmètre restent hors de portée.
 */
public record ProfileUpdateRequest(

        @NotBlank(message = "Le nom est obligatoire")
        @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
        String nom,

        @NotBlank(message = "Le prénom est obligatoire")
        @Size(min = 2, max = 150, message = "Le prénom doit contenir entre 2 et 150 caractères")
        String prenom,

        @Email(message = "L'adresse e-mail est invalide")
        @Size(max = 150, message = "L'adresse e-mail ne doit pas dépasser 150 caractères")
        String email,

        @Size(max = 50, message = "Le téléphone ne doit pas dépasser 50 caractères")
        String telephone
) {
}
