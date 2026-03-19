package com.eyram.dev.church_project_spring.DTO.request;

import com.eyram.dev.church_project_spring.enums.ModePaiement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TypePaiementRequest(
        @NotBlank(message = "Le libellé est obligatoire")
        String libelle,

        @NotNull(message = "Le mode est obligatoire")
        ModePaiement mode
) {
}