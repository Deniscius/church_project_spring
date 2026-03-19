package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.TypeDemandeEnum;

import java.time.LocalDateTime;
import java.util.UUID;

public record TypeDemandeResponse(
        UUID publicId,
        String libelle,
        String description,
        TypeDemandeEnum typeDemandeEnum,
        Boolean isActive,
        UUID paroissePublicId,
        String paroisseNom,
        Boolean statusDel,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}