package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Correction ciblée de l'intention : le secrétariat reformule sans toucher
 * au forfait, aux dates ni au paiement.
 */
public record DemandeIntentionRequest(
        @NotBlank(message = "L'intention est obligatoire")
        @Size(max = 500, message = "L'intention ne doit pas dépasser 500 caractères")
        String intention
) {
}
