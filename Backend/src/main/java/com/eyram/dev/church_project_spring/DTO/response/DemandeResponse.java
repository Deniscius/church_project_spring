package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.ModePaiement;
import com.eyram.dev.church_project_spring.enums.NatureForfaitEnum;
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

@JsonInclude(JsonInclude.Include.NON_NULL)
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
        NatureForfaitEnum natureForfait,
        boolean validationRequise,
        boolean paiementDisponible,
        String paiementIndisponibleMotif,
        LocalDateTime premiereCelebrationAt,

        UUID horairePublicId,
        String horaireLibelle,
        /** Heure de l'horaire paroissial retenu (null si heure personnalisée). */
        LocalTime horaireHeure,

        UUID userPublicId,
        String username,

        UUID typePaiementPublicId,
        String typePaiementLibelle,
        ModePaiement modePaiement,

        Boolean statusDel,
        /** Soft delete : date/heure de retrait des listes actives. */
        LocalDateTime deletedAt,
        /** Soft delete : auteur de la suppression. */
        String deletedByNom,
        /** Date/heure de dépôt de la demande (pas la célébration). */
        LocalDateTime createdAt,
        LocalDateTime updatedAt,

        List<LocalDate> datesCelebration,

        /** Détail par date (heure / horaire) — essentiel pour neuvaine / triduum. */
        List<CelebrationSlotResponse> celebrationSlots,

        UUID facturePublicId,
        String refFacture,
        LocalDateTime dateDetailsPaiement,
        String idTransaction,
        String numero
) {
}
