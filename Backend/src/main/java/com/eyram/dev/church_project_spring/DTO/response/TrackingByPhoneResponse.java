package com.eyram.dev.church_project_spring.DTO.response;

import java.util.List;

/**
 * Résultat de recherche publique par téléphone.
 * Les demandes sont renvoyées dans l'ordre chronologique de dépôt.
 */
public record TrackingByPhoneResponse(
        List<String> codes,
        List<TrackingByPhoneItemResponse> demandes,
        int count
) {
    /** Compatibilité avec le service legacy qui ne renvoie encore que les codes. */
    public TrackingByPhoneResponse(List<String> codes, int count) {
        this(codes, List.of(), count);
    }
}
