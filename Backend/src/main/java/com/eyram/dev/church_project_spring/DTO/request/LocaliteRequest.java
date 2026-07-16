package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LocaliteRequest(

        @NotBlank(message = "La ville est obligatoire")
        @Size(min = 2, max = 150, message = "La ville doit contenir entre 2 et 150 caractères")
        String ville,

        @NotBlank(message = "Le quartier est obligatoire")
        @Size(min = 2, max = 200, message = "Le quartier doit contenir entre 2 et 200 caractères")
        String quartier

) {
}
