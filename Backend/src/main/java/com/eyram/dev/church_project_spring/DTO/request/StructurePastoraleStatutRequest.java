package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record StructurePastoraleStatutRequest(
        @NotNull(message = "Le statut est obligatoire")
        Boolean actif,

        @NotNull(message = "La version est obligatoire")
        @PositiveOrZero(message = "La version doit être positive ou nulle")
        Long version
) {
}
