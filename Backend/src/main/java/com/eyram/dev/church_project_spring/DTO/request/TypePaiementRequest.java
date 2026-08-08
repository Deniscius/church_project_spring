package com.eyram.dev.church_project_spring.DTO.request;

import com.eyram.dev.church_project_spring.enums.ModePaiement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TypePaiementRequest(
        @NotBlank(message = "Le libellé est obligatoire")
        @Size(max = 150, message = "Le libellé ne doit pas dépasser 150 caractères")
        String libelle,

        @NotNull(message = "Le mode est obligatoire")
        ModePaiement mode
) {
}
