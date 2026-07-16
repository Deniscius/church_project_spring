package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ParoisseRequest(

        @NotBlank(message = "Le nom est obligatoire")
        @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
        String nom,

        @NotBlank(message = "L'adresse est obligatoire")
        @Size(min = 3, max = 200, message = "L'adresse doit contenir entre 3 et 200 caractères")
        String adresse,

        @Email(message = "L'email est invalide")
        @Size(max = 150, message = "L'email ne doit pas dépasser 150 caractères")
        String email,

        @Size(max = 50, message = "Le téléphone ne doit pas dépasser 50 caractères")
        String telephone,

        @NotNull(message = "La localité est obligatoire")
        UUID localitePublicId

) {
}
