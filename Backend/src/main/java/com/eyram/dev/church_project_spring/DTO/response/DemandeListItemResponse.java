package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.StatutDemandeEnum;
import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;
import com.eyram.dev.church_project_spring.enums.StatutValidationEnum;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Projection légère pour les listes/recherches admin de demandes.
 * Évite de charger les données de facture/paiement détaillées inutiles à la grille.
 */
public record DemandeListItemResponse(
        UUID publicId,
        String codeSuivie,
        String nomFidele,
        String prenomFidele,
        String telFidele,
        String typeDemandeLibelle,
        StatutDemandeEnum statutDemande,
        StatutValidationEnum statutValidation,
        StatutPaiementEnum statutPaiement,
        BigDecimal montant,
        Boolean statusDel,
        LocalDateTime deletedAt,
        String deletedByNom,
        LocalDateTime createdAt,
        List<LocalDate> datesCelebration
) {
}
