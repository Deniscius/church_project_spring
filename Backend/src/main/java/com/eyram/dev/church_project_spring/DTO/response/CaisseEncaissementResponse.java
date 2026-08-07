package com.eyram.dev.church_project_spring.DTO.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ligne de caisse locale : encaissement espèces resté dans la paroisse,
 * jamais versé dans le solde de reversement plateforme.
 */
public record CaisseEncaissementResponse(
        UUID publicId,
        LocalDateTime dateEncaissement,
        Integer montant,
        String codeSuivie,
        String intention,
        String fidele,
        String refFacture,
        String idTransaction,
        String encaisseurNom
) {
}
