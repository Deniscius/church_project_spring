package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.PlanAbonnement;
import com.eyram.dev.church_project_spring.enums.StatutAbonnement;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Période d'abonnement d'une paroisse, vue du comptable plateforme.
 *
 * @param joursRestants jours avant l'échéance ; négatif une fois le terme passé
 * @param enTolerance   échéance dépassée mais accès encore ouvert
 * @param echeanceProche échéance dans la fenêtre d'alerte configurée
 */
public record AbonnementResponse(
        UUID publicId,
        UUID paroissePublicId,
        String paroisseNom,
        String doyenneNom,
        String paroisseEmail,
        String paroisseTelephone,
        boolean paroisseActive,

        PlanAbonnement plan,
        Integer montant,
        StatutAbonnement statut,
        LocalDateTime debutAt,
        LocalDateTime finAt,
        LocalDateTime activatedAt,
        String activationSource,
        String activatedByNom,
        Long joursRestants,
        boolean enTolerance,
        boolean echeanceProche,

        String idTransaction,
        String paymentUrl,
        LocalDateTime createdAt
) {
}
