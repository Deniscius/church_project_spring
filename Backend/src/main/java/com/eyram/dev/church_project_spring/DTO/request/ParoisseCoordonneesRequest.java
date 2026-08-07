package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Coordonnées qu'une paroisse tient elle-même à jour.
 * <p>
 * Le RIB conditionne toute demande de reversement : le laisser sous la seule
 * main du super admin bloquait les paroisses. Le nom, l'adresse et le doyenné
 * restent en revanche du ressort de la plateforme.
 */
public record ParoisseCoordonneesRequest(

        @Email(message = "L'adresse e-mail est invalide")
        @Size(max = 150, message = "L'adresse e-mail ne doit pas dépasser 150 caractères")
        String email,

        @Size(max = 50, message = "Le téléphone ne doit pas dépasser 50 caractères")
        String telephone,

        @Size(max = 120, message = "Le nom de la banque ne doit pas dépasser 120 caractères")
        String nomBanque,

        @Size(max = 150, message = "Le titulaire ne doit pas dépasser 150 caractères")
        String titulaireCompte,

        @Size(max = 80, message = "Le RIB / IBAN ne doit pas dépasser 80 caractères")
        String ibanOrRib
) {
}
