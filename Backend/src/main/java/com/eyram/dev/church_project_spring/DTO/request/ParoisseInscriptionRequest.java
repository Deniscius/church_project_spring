package com.eyram.dev.church_project_spring.DTO.request;

import com.eyram.dev.church_project_spring.enums.RoleParoisse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record ParoisseInscriptionRequest(
        @NotBlank @Size(max = 100) String nomParoisse,
        @NotBlank @Size(max = 200) String adresse,
        @Email @Size(max = 150) String email,
        @Size(max = 50) String telephone,
        @NotNull UUID doyennePublicId,
        @NotBlank @Size(max = 30) String planAbonnement,
        @NotBlank @Size(max = 80) String adminNom,
        @NotBlank @Size(max = 80) String adminPrenom,
        /** E-mail personnel vérifié par OTP — requis. */
        @NotBlank @Email @Size(max = 150) String adminEmail,
        @Size(max = 50) String adminTelephone,
        /**
         * Identifiant annoncé après OTP (doit correspondre à la preuve serveur).
         * Le mot de passe n'est jamais accepté du client : il est pris depuis {@code otpProof}.
         */
        @NotBlank @Size(max = 80) String adminUsername,
        /** Preuve OTP renvoyée par /otp/verifier (porte le mot de passe côté serveur). */
        @NotBlank @Size(max = 80) String otpProof,
        @Size(max = 500) String message,
        @Valid List<MembreRequest> membres
) {
    public record MembreRequest(
            @NotBlank @Size(max = 80) String nom,
            @NotBlank @Size(max = 80) String prenom,
            @Email @Size(max = 150) String email,
            @Size(max = 50) String telephone,
            @NotNull RoleParoisse roleParoisse,
            @Size(max = 80) String username
    ) {
    }
}
