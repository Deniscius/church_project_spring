package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DoyenneRequest(

        @NotBlank(message = "Le nom du doyenné est obligatoire")
        @Size(min = 2, max = 400, message = "Le nom doit contenir entre 2 et 400 caractères")
        String nom,

        @Size(max = 500, message = "La description ne doit pas dépasser 500 caractères")
        String description,

        /** Position dans l'ordre diocésain ; à défaut, le doyenné est placé en dernier. */
        @Min(value = 1, message = "Le rang doit être supérieur ou égal à 1")
        Integer rang

) {
}
