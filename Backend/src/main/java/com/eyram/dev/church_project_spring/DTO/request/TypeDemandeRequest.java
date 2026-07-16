package com.eyram.dev.church_project_spring.DTO.request;

import com.eyram.dev.church_project_spring.enums.TypeDemandeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.UUID;

public record TypeDemandeRequest(

        @NotBlank(message = "Le libellé est obligatoire")
        String libelle,

        String description,

        @NotNull(message = "Le type de demande est obligatoire")
        TypeDemandeEnum typeDemandeEnum,

        @NotNull(message = "L'état d'activation est obligatoire")
        Boolean isActive,

        @NotNull(message = "Le délai minimum est obligatoire")
        @Min(value = 0, message = "Le délai minimum ne peut pas être négatif")
        @Max(value = 8760, message = "Le délai minimum ne peut pas dépasser un an")
        Integer delaiMinimumHeures,

        @NotNull(message = "La paroisse est obligatoire")
        UUID paroissePublicId
) {
}
