package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.StatutTenant;

import java.time.LocalDateTime;
import java.util.UUID;

public record ParoisseResponse(
        UUID publicId,
        String nom,
        String adresse,
        String email,
        String telephone,
        Boolean isActive,
        Boolean isSystem,
        StatutTenant statutTenant,
        String nomBanque,
        String titulaireCompte,
        String ibanOrRib,
        /** Présence d'un logo personnalisé pour les reçus. */
        boolean logoPresent,
        LocalDateTime subscriptionExpiresAt,
        UUID doyennePublicId,
        String doyenneNom,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
