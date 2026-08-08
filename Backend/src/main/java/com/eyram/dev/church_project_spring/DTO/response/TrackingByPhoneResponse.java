package com.eyram.dev.church_project_spring.DTO.response;

import java.util.List;

/**
 * Résultat de recherche publique par téléphone : codes de suivi uniquement
 * (pas d'intention ni de PII supplémentaire).
 */
public record TrackingByPhoneResponse(
        List<String> codes,
        int count
) {
}
