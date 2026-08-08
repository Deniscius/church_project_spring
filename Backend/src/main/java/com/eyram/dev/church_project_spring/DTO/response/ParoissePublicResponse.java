package com.eyram.dev.church_project_spring.DTO.response;

import java.util.UUID;

/**
 * Vue publique d'une paroisse (fidèle) — sans RIB ni données bancaires.
 */
public record ParoissePublicResponse(
        UUID publicId,
        String nom,
        String adresse,
        Boolean isActive,
        UUID doyennePublicId,
        String doyenneNom
) {
}
