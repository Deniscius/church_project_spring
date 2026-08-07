package com.eyram.dev.church_project_spring.DTO.response;

import java.util.UUID;

/**
 * Vue publique minimale d'une paroisse de l'annuaire diocésain, destinée au
 * formulaire d'inscription. Volontairement dépouillée de tout ce qui n'aide pas
 * une paroisse à se reconnaître : ni coordonnées bancaires, ni état d'abonnement.
 */
public record AnnuaireParoisseResponse(
        UUID publicId,
        String nom,
        String adresse
) {
}
