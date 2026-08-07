package com.eyram.dev.church_project_spring.DTO.response;

import java.util.UUID;

public record CompteParoisseResponse(
        UUID publicId,
        UUID paroissePublicId,
        String paroisseNom,
        int soldeDisponible,
        int soldeEnAttente
) {
}
