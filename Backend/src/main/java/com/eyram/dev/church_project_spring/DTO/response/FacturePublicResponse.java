package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.ModePaiement;
import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;

import java.time.LocalDateTime;

/**
 * Facture publique (code de suivi) : montants / statuts, PII masquée, pas d'IDs provider.
 */
public record FacturePublicResponse(
        String refFacture,
        LocalDateTime dateEmission,
        LocalDateTime datePaiement,
        Integer montant,
        StatutPaiementEnum statutPaiement,
        String codeSuivieDemande,
        String prenomFidele,
        String nomFideleMasked,
        String telFideleMasked,
        String intention,
        String typeDemandeLibelle,
        String forfaitNom,
        String typePaiementLibelle,
        ModePaiement modePaiement,
        FacturePublicReglementResponse reglement
) {
}
