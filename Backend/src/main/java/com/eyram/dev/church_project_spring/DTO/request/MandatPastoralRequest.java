package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record MandatPastoralRequest(
        @NotBlank(message = "La fonction est obligatoire")
        @Size(max = 120, message = "La fonction ne doit pas dépasser 120 caractères")
        String fonction,

        @Size(max = 1500, message = "Les attributions ne doivent pas dépasser 1500 caractères")
        String attributions,

        LocalDate dateDebut,
        LocalDate dateFin,

        @Min(value = 0, message = "L'ordre d'affichage doit être positif ou nul")
        Integer ordreAffichage,

        @NotNull(message = "La paroisse est obligatoire")
        UUID paroissePublicId,

        @NotNull(message = "L'année pastorale est obligatoire")
        UUID anneePastoralePublicId,

        @NotNull(message = "L'acteur pastoral est obligatoire")
        UUID acteurPastoralPublicId,

        @NotNull(message = "La structure pastorale est obligatoire")
        UUID structurePastoralePublicId,

        @PositiveOrZero(message = "La version doit être positive ou nulle")
        Long version
) {
}
