package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.JourSemaine;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record HoraireResponse(
        UUID publicId,
        JourSemaine jourSemaine,
        LocalTime heureCelebration,
        String libelle,
        Boolean isActive,
        UUID paroissePublicId,
        String paroisseNom,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}