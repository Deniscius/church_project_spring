package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record AnneePastoraleRequest(
        @NotBlank(message = "Le libellé est obligatoire")
        @Size(max = 100, message = "Le libellé ne doit pas dépasser 100 caractères")
        String libelle,

        @NotNull(message = "La date de début est obligatoire")
        LocalDate dateDebut,

        @NotNull(message = "La date de fin est obligatoire")
        LocalDate dateFin,

        @Size(max = 1000, message = "La description ne doit pas dépasser 1000 caractères")
        String description,

        @NotNull(message = "La paroisse est obligatoire")
        UUID paroissePublicId,

        /** Obligatoire lors d'une modification afin de détecter une version obsolète. */
        @PositiveOrZero(message = "La version doit être positive ou nulle")
        Long version
) {
}
