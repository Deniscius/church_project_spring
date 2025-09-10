package com.eyram.dev.church_project_spring.DTO.request;

import java.time.LocalDate;
import java.util.UUID;

public record  DemandeRequest(
        String intention,
        LocalDate dateDemande,
        UUID clientPublicId,
        UUID typeDemandePublicId
) {}
