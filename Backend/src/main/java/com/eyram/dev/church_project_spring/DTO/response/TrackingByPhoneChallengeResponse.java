package com.eyram.dev.church_project_spring.DTO.response;

import java.util.List;

/**
 * Suivi par téléphone :
 * <ul>
 *   <li>si un e-mail est lié → OTP envoyé ({@code demandes} vide) ;</li>
 *   <li>sinon → résumés des demandes renvoyés directement.</li>
 * </ul>
 */
public record TrackingByPhoneChallengeResponse(
        String emailMasked,
        int expiresInSeconds,
        String message,
        List<String> codes,
        List<TrackingByPhoneItemResponse> demandes
) {
}
