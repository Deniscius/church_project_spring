package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LocaliteRequest(

        @NotBlank(message = "La ville est obligatoire")
        @Size(max = 150, message = "La ville ne doit pas dépasser 150 caractères")
        String ville,

        @NotBlank(message = "Le quartier est obligatoire")
        @Size(max = 200, message = "Le quartier ne doit pas dépasser 200 caractères")
        String quartier

) {
}