package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.ModePaiement;
import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;

import java.time.LocalDateTime;
import java.util.UUID;

public record FactureResponse(
        UUID publicId,
        String refFacture,
        LocalDateTime dateEmission,
        LocalDateTime datePaiement,
        Integer montant,
        StatutPaiementEnum statutPaiement,

        UUID demandePublicId,
        String codeSuivieDemande,
        String nomFidele,
        String prenomFidele,
        String telFidele,
        String emailFidele,
        String intention,
        String typeDemandeLibelle,
        String forfaitNom,
        String statutDemande,

        UUID typePaiementPublicId,
        String typePaiementLibelle,
        ModePaiement modePaiement,

        /** Détail de l'encaissement ; {@code null} tant que la facture n'est pas payée. */
        PaiementReglementResponse reglement
) {

    public FactureResponse withReglement(PaiementReglementResponse value) {
        return new FactureResponse(
                publicId, refFacture, dateEmission, datePaiement, montant, statutPaiement,
                demandePublicId, codeSuivieDemande, nomFidele, prenomFidele, telFidele, emailFidele,
                intention, typeDemandeLibelle, forfaitNom, statutDemande,
                typePaiementPublicId, typePaiementLibelle, modePaiement,
                value
        );
    }
}
