package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.ModePaiement;
import com.eyram.dev.church_project_spring.enums.StatutDemandeEnum;
import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;
import com.eyram.dev.church_project_spring.enums.StatutValidationEnum;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Vue fidèle / publique d'une demande : pas d'IDs internes, usernames, ni PII claire.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DemandePublicResponse(
        String codeSuivie,
        String intention,
        String prenomFidele,
        String nomFideleMasked,
        String telFideleMasked,
        String emailFideleMasked,
        BigDecimal montant,
        StatutPaiementEnum statutPaiement,
        StatutValidationEnum statutValidation,
        StatutDemandeEnum statutDemande,
        String paroisseNom,
        String typeDemandeLibelle,
        String forfaitTarifNom,
        String horaireLibelle,
        LocalTime horaireHeure,
        UUID typePaiementPublicId,
        String typePaiementLibelle,
        ModePaiement modePaiement,
        LocalDateTime createdAt,
        List<LocalDate> datesCelebration,
        List<CelebrationSlotResponse> celebrationSlots
) {
}
