package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrackingByPhoneRequest(
        @NotBlank(message = "Le numéro de téléphone est obligatoire")
        @Size(min = 8, max = 30)
        String telephone
) {
}
