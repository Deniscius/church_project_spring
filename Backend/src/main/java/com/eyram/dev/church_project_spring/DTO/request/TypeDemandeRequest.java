package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record TypeDemandeRequest(

        @NotBlank(message = "Le libellé est obligatoire")
        @Size(max = 150, message = "Le libellé ne doit pas dépasser 150 caractères")
        String libelle,

        @Size(max = 255, message = "La description ne doit pas dépasser 255 caractères")
        String description,

        @NotNull(message = "Le statut actif est obligatoire")
        Boolean isActive,

        @NotNull(message = "La paroisse est obligatoire")
        UUID paroissePublicId

) {
}