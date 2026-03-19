package com.eyram.dev.church_project_spring.DTO.request;

import com.eyram.dev.church_project_spring.enums.TypeDemandeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TypeDemandeRequest(

        @NotBlank(message = "Le libellé est obligatoire")
        String libelle,

        String description,

        @NotNull(message = "Le type de demande est obligatoire")
        TypeDemandeEnum typeDemandeEnum,

        @NotNull(message = "L'état d'activation est obligatoire")
        Boolean isActive,

        @NotNull(message = "La paroisse est obligatoire")
        UUID paroissePublicId
) {
}