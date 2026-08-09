package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.response.DemandePublicResponse;
import com.eyram.dev.church_project_spring.DTO.response.DemandeResponse;
import com.eyram.dev.church_project_spring.DTO.response.FacturePublicReglementResponse;
import com.eyram.dev.church_project_spring.DTO.response.FacturePublicResponse;
import com.eyram.dev.church_project_spring.DTO.response.FactureResponse;
import com.eyram.dev.church_project_spring.DTO.response.PaiementReglementResponse;
import com.eyram.dev.church_project_spring.utils.PiiMasking;
import org.springframework.stereotype.Service;

@Service
public class PublicDemandeViewService {

    public DemandePublicResponse toPublic(DemandeResponse full) {
        if (full == null) {
            return null;
        }
        return new DemandePublicResponse(
                full.codeSuivie(),
                full.intention(),
                full.prenomFidele(),
                maskName(full.nomFidele()),
                PiiMasking.maskPhone(full.telFidele()),
                PiiMasking.maskEmail(full.emailFidele()),
                full.montant(),
                full.statutPaiement(),
                full.statutValidation(),
                full.statutDemande(),
                full.paroisseNom(),
                full.typeDemandeLibelle(),
                full.forfaitTarifNom(),
                full.horaireLibelle(),
                full.horaireHeure(),
                full.typePaiementPublicId(),
                full.typePaiementLibelle(),
                full.modePaiement(),
                full.createdAt(),
                full.datesCelebration(),
                full.celebrationSlots()
        );
    }

    public FacturePublicResponse toPublic(FactureResponse full) {
        if (full == null) {
            return null;
        }
        return new FacturePublicResponse(
                full.refFacture(),
                full.dateEmission(),
                full.datePaiement(),
                full.montant(),
                full.statutPaiement(),
                full.codeSuivieDemande(),
                full.prenomFidele(),
                maskName(full.nomFidele()),
                PiiMasking.maskPhone(full.telFidele()),
                full.intention(),
                full.typeDemandeLibelle(),
                full.forfaitNom(),
                full.typePaiementLibelle(),
                full.modePaiement(),
                toPublicReglement(full.reglement())
        );
    }

    private static FacturePublicReglementResponse toPublicReglement(PaiementReglementResponse r) {
        if (r == null) {
            return null;
        }
        return new FacturePublicReglementResponse(
                r.datePaiement(),
                r.statut() == null ? null : r.statut().name(),
                r.montantCharge(),
                r.montantFrais(),
                r.montantFraisAgregateur(),
                r.montantFraisPlateforme(),
                r.montantNet(),
                r.provider()
        );
    }

    private static String maskName(String nom) {
        if (nom == null || nom.isBlank()) {
            return null;
        }
        String t = nom.trim();
        if (t.length() <= 1) {
            return "*";
        }
        return t.charAt(0) + "•••";
    }
}
