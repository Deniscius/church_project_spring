package com.eyram.dev.church_project_spring.DTO.request;

import com.eyram.dev.church_project_spring.enums.TypeStructurePastorale;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record StructurePastoraleRequest(
        @NotNull(message = "Le type de structure est obligatoire")
        TypeStructurePastorale type,

        @NotBlank(message = "Le nom de la structure est obligatoire")
        @Size(max = 150, message = "Le nom ne doit pas dépasser 150 caractères")
        String nom,

        @Size(max = 1500, message = "Les attributions ne doivent pas dépasser 1500 caractères")
        String attributions,

        @Min(value = 0, message = "L'ordre d'affichage doit être positif ou nul")
        Integer ordreAffichage,

        @NotNull(message = "La paroisse est obligatoire")
        UUID paroissePublicId,

        @PositiveOrZero(message = "La version doit être positive ou nulle")
        Long version
) {
}
