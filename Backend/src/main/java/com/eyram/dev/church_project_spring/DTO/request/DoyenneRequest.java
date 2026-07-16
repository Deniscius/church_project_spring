package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DoyenneRequest(

        @NotBlank(message = "Le nom du doyenné est obligatoire")
        @Size(min = 2, max = 400, message = "Le nom doit contenir entre 2 et 400 caractères")
        String nom,

        @Size(max = 500, message = "La description ne doit pas dépasser 500 caractères")
        String description

) {
}
