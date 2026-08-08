package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InscriptionOtpVerifyRequest(
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 4, max = 12) String code,
        @NotBlank @Size(max = 80) String adminPrenom,
        @NotBlank @Size(max = 80) String adminNom,
        /** Nom de la paroisse : sert à construire un identifiant lisible (ex. admin.saint-joseph). */
        @Size(max = 100) String nomParoisse
) {
}
