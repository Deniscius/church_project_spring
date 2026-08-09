package com.eyram.dev.church_project_spring.DTO.response;

/**
 * Réponse OTP : jamais de mot de passe (uniquement e-mail).
 * Le secret reste côté serveur dans la preuve {@code otpProof}.
 */
public record InscriptionOtpVerifyResponse(
        String email,
        String otpProof,
        String adminUsername,
        int expiresInSeconds,
        String message
) {
}
