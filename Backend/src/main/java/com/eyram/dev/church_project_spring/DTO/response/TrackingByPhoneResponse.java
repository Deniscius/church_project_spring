package com.eyram.dev.church_project_spring.DTO.response;

import java.util.List;

/**
 * Résultat public minimal d'une recherche par téléphone.
 * Aucun renseignement personnel ni détail de demande n'est exposé ici.
 */
public record TrackingByPhoneResponse(
        List<String> codes,
        int count
) {
}
