package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.ModePaiement;
import com.eyram.dev.church_project_spring.enums.StatutDemandeEnum;
import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;
import com.eyram.dev.church_project_spring.enums.StatutValidationEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record DemandeResponse(
        UUID publicId,
        String intention,
        String codeSuivie,
        String nomFidele,
        String prenomFidele,
        String telFidele,
        String emailFidele,
        BigDecimal montant,
        String nomCoursier,
        StatutPaiementEnum statutPaiement,
        StatutValidationEnum statutValidation,
        String validateBy,
        LocalTime heurePersonnalisee,
        StatutDemandeEnum statutDemande,

        UUID paroissePublicId,
        String paroisseNom,

        UUID typeDemandePublicId,
        String typeDemandeLibelle,

        UUID forfaitTarifPublicId,
        String forfaitTarifNom,

        UUID horairePublicId,
        String horaireLibelle,

        UUID userPublicId,
        String username,

        UUID typePaiementPublicId,
        String typePaiementLibelle,
        ModePaiement modePaiement,

        Boolean statusDel,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}