package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Correction ciblée de l'intention : le secrétariat reformule sans toucher
 * au forfait, aux dates ni au paiement.
 */
public record DemandeIntentionRequest(
        @NotBlank(message = "L'intention est obligatoire")
        @Size(min = 10, max = 500, message = "L'intention doit contenir entre 10 et 500 caractères")
        String intention
) {
}
