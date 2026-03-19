package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record ForfaitTarifRequest(

        @NotBlank(message = "Le code du forfait est obligatoire")
        String codeForfait,

        @NotBlank(message = "Le nom du forfait est obligatoire")
        String nomForfait,

        @NotNull(message = "Le montant du forfait est obligatoire")
        @Positive(message = "Le montant du forfait doit être supérieur à zéro")
        BigDecimal montantForfait,

        Integer nombreJour,

        Integer nombreCelebration,

        Integer joursAutorise,

        @NotNull(message = "Le champ heure personnalisée est obligatoire")
        Boolean heurePersonnalise,

        String libelle,

        @NotNull(message = "L'état d'activation est obligatoire")
        Boolean isActive,

        @NotNull(message = "Le type de demande est obligatoire")
        UUID typeDemandePublicId
) {
}