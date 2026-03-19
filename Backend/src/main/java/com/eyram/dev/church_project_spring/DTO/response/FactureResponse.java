package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;

import java.time.LocalDateTime;
import java.util.UUID;

public record FactureResponse(
        UUID publicId,
        String refFacture,
        LocalDateTime datePaiement,
        Integer montant,
        StatutPaiementEnum statutPaiement,
        UUID demandePublicId,
        String codeSuivieDemande,
        String nomFidele,
        String prenomFidele
) {
}