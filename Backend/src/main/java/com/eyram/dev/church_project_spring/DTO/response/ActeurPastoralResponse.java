package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.CategorieActeurPastoral;

import java.time.LocalDateTime;
import java.util.UUID;

public record ActeurPastoralResponse(
        UUID publicId,
        String nom,
        String prenoms,
        String appellation,
        CategorieActeurPastoral categorie,
        String telephone,
        String email,
        String notes,
        Boolean actif,
        Long version,
        UUID paroissePublicId,
        String paroisseNom,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
