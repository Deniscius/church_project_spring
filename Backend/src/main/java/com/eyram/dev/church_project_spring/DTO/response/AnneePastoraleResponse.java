package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.StatutAnneePastorale;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record AnneePastoraleResponse(
        UUID publicId,
        String libelle,
        LocalDate dateDebut,
        LocalDate dateFin,
        String description,
        StatutAnneePastorale statut,
        LocalDateTime publishedAt,
        LocalDateTime closedAt,
        Long version,
        UUID paroissePublicId,
        String paroisseNom,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
