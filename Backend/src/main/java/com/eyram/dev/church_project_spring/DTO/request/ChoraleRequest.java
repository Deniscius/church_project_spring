package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChoraleRequest(
        @NotBlank(message = "Le nom est obligatoire")
        @Size(min = 2, max = 150, message = "Le nom doit contenir entre 2 et 150 caractères")
        String nom,
        @Size(max = 500, message = "La description ne doit pas dépasser 500 caractères")
        String description
) {}
