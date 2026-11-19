package com.eyram.dev.church_project_spring.DTO.response;

import java.time.LocalDateTime;
import java.util.UUID;

/** Lightweight dashboard row: no invoice, payment history or collection enrichment. */
public record UnpaidApproachingResponse(
    UUID publicId,
    String codeSuivie,
    String nomFidele,
    String prenomFidele,
    String typeDemandeLibelle,
    LocalDateTime premiereCelebration) {}
