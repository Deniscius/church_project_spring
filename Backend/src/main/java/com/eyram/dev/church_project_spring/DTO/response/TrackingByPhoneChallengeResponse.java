package com.eyram.dev.church_project_spring.DTO.response;

import java.util.List;

/**
 * Réponse uniforme au démarrage du suivi par téléphone.
 *
 * <p>Les listes restent toujours vides afin de ne pas révéler publiquement
 * l'existence d'un numéro ni les demandes qui lui sont associées.</p>
 */
public record TrackingByPhoneChallengeResponse(
        String emailMasked,
        int expiresInSeconds,
        String message,
        List<String> codes,
        List<TrackingByPhoneItemResponse> demandes
) {
}
