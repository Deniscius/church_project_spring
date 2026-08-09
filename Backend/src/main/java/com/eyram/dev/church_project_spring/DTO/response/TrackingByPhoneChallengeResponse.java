package com.eyram.dev.church_project_spring.DTO.response;

import java.util.List;

/**
 * Suivi par téléphone :
 * <ul>
 *   <li>si un e-mail est lié → OTP envoyé ({@code codes} vide) ;</li>
 *   <li>sinon → codes renvoyés directement (téléphone seul, cas le plus fréquent).</li>
 * </ul>
 */
public record TrackingByPhoneChallengeResponse(
        String emailMasked,
        int expiresInSeconds,
        String message,
        List<String> codes
) {
}
