package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PlanSaasRequest(
        @NotBlank(message = "Le nom du plan est obligatoire")
        @Size(max = 100, message = "Le nom du plan ne peut pas dépasser 100 caractères")
        String nom,

        @Size(max = 300, message = "La description ne peut pas dépasser 300 caractères")
        String description,

        @NotNull(message = "Le montant est obligatoire")
        @Positive(message = "Le montant doit être supérieur à zéro")
        Integer montantXof,

        @NotNull(message = "La durée est obligatoire")
        @Positive(message = "La durée doit être supérieure à zéro")
        Integer dureeMois,

        @NotNull(message = "L'état actif est obligatoire")
        Boolean actif,

        @NotNull(message = "L'indicateur de mise en avant est obligatoire")
        Boolean featured,

        @NotNull(message = "L'ordre d'affichage est obligatoire")
        @PositiveOrZero(message = "L'ordre d'affichage doit être positif ou nul")
        Integer ordreAffichage
) {
}
