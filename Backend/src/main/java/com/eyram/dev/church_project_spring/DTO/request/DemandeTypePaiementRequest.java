package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DemandeTypePaiementRequest(
        @NotNull(message = "Le type de paiement est obligatoire")
        UUID typePaiementPublicId
) {
}
