package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ParoisseRequest(

        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
        String nom,

        @NotBlank(message = "L'adresse est obligatoire")
        @Size(max = 200, message = "L'adresse ne doit pas dépasser 200 caractères")
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