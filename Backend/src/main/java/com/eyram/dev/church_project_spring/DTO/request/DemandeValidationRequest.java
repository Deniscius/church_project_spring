package com.eyram.dev.church_project_spring.DTO.request;

import com.eyram.dev.church_project_spring.enums.StatutValidationEnum;
import jakarta.validation.constraints.NotNull;

public record DemandeValidationRequest(
        @NotNull(message = "Le statut de validation est obligatoire")
        StatutValidationEnum statut
) {
}
