package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.RoleParoisse;
import com.eyram.dev.church_project_spring.enums.StatutInscription;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ParoisseInscriptionResponse(
        UUID publicId,
        String nomParoisse,
        String adresse,
        String email,
        String telephone,
        UUID doyennePublicId,
        String doyenneNom,
        String planAbonnement,
        int montantAbonnement,
        String adminNom,
        String adminPrenom,
        String adminEmail,
        String adminTelephone,
        String adminUsername,
        StatutInscription statut,
        String message,
        UUID paroissePublicId,
        /** Renseignés une fois la paroisse créée : suivi de l'activation par le comptable. */
        Boolean paroisseActive,
        LocalDateTime abonnementFinAt,
        LocalDateTime createdAt,
        /** true si le mandat du curé a été déposé. */
        boolean mandatCurePresent,
        /** true si la CNI / pièce d'identité admin a été déposée. */
        boolean adminCniPresent,
        List<MembreResponse> membres
) {
    public record MembreResponse(
            String nom,
            String prenom,
            String email,
            String telephone,
            RoleParoisse roleParoisse,
            String username
    ) {
    }
}
