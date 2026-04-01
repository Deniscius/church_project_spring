package com.eyram.dev.church_project_spring.security.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Le nom d'utilisateur est obligatoire")
        String username,
        @NotBlank(message = "Le mot de passe est obligatoire")
        String password
) {
}
