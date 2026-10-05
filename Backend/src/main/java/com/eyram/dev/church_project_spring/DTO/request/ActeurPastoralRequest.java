package com.eyram.dev.church_project_spring.DTO.request;

import com.eyram.dev.church_project_spring.enums.CategorieActeurPastoral;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ActeurPastoralRequest(
        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
        String nom,

        @NotBlank(message = "Les prénoms sont obligatoires")
        @Size(max = 150, message = "Les prénoms ne doivent pas dépasser 150 caractères")
        String prenoms,

        @Size(max = 50, message = "L'appellation ne doit pas dépasser 50 caractères")
        String appellation,

        @NotNull(message = "La catégorie est obligatoire")
        CategorieActeurPastoral categorie,

        @Size(max = 50, message = "Le téléphone ne doit pas dépasser 50 caractères")
        String telephone,

        @Email(message = "L'adresse email est invalide")
        @Size(max = 150, message = "L'adresse email ne doit pas dépasser 150 caractères")
        String email,

        @Size(max = 1000, message = "Les notes ne doivent pas dépasser 1000 caractères")
        String notes,

        @NotNull(message = "La paroisse est obligatoire")
        UUID paroissePublicId,

        @PositiveOrZero(message = "La version doit être positive ou nulle")
        Long version
) {
}
