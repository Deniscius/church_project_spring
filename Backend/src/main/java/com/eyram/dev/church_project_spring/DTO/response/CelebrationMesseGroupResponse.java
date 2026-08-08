package com.eyram.dev.church_project_spring.DTO.response;

import java.time.LocalTime;
import java.util.List;

/**
 * Bloc d'intentions pour une même heure de messe (ex. messe de 06:00).
 */
public record CelebrationMesseGroupResponse(
        LocalTime heure,
        String libelleMesse,
        int nombreIntentions,
        List<CelebrationIntentionResponse> intentions
) {
}
