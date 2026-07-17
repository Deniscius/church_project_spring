package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.JourSemaine;
import com.eyram.dev.church_project_spring.enums.TypeDemandeEnum;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record TypeDemandeResponse(
        UUID publicId,
        String libelle,
        String description,
        TypeDemandeEnum typeDemandeEnum,
        Boolean isActive,
        Integer delaiMinimumHeures,
        Set<JourSemaine> joursCelebrationAutorises,
        UUID paroissePublicId,
        String paroisseNom,
        Boolean statusDel,
        String statutLabel,
        String resume,
        String disponibiliteLabel,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
