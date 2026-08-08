package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(
        @NotBlank(message = "Identifiant ou e-mail obligatoire")
        @Size(max = 150)
        String usernameOrEmail
) {
}
