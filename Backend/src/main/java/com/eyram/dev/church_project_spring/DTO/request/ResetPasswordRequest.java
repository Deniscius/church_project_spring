package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Le jeton est obligatoire")
        @Size(min = 20, max = 200)
        String token,

        @NotBlank(message = "Le nouveau mot de passe est obligatoire")
        @Size(min = 8, max = 200, message = "Le mot de passe doit contenir entre 8 et 200 caractères")
        String newPassword
) {
}
