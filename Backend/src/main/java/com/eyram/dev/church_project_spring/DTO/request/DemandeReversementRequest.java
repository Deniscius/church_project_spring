package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record DemandeReversementRequest(
        @NotNull UUID paroissePublicId,
        @NotNull @Min(100) Integer montant,
        @Size(max = 500) String motif
) {
}
