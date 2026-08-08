package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.StatutReversement;

import java.time.LocalDateTime;
import java.util.UUID;

public record DemandeReversementResponse(
        UUID publicId,
        UUID paroissePublicId,
        String paroisseNom,
        Integer montant,
        StatutReversement statut,
        String nomBanque,
        String titulaireCompte,
        String ibanOrRib,
        String motif,
        String referenceVirement,
        String traitePar,
        LocalDateTime traiteAt,
        LocalDateTime createdAt
) {
}
