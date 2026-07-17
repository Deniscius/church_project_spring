package com.eyram.dev.church_project_spring.DTO.request;

import com.eyram.dev.church_project_spring.enums.JourSemaine;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record ForfaitTarifRequest(

        @NotBlank(message = "Le code du forfait est obligatoire")
        @Size(min = 2, max = 50, message = "Le code du forfait doit contenir entre 2 et 50 caractères")
        String codeForfait,

        @NotBlank(message = "Le nom du forfait est obligatoire")
        @Size(min = 2, max = 150, message = "Le nom du forfait doit contenir entre 2 et 150 caractères")
        String nomForfait,

        @NotNull(message = "Le montant du forfait est obligatoire")
        @Positive(message = "Le montant du forfait doit être supérieur à zéro")
        BigDecimal montantForfait,

        Integer nombreJour,

        Integer nombreCelebration,

        @NotEmpty(message = "Sélectionnez au moins un jour de célébration compatible")
        Set<JourSemaine> joursCelebrationAutorises,

        @NotNull(message = "Le champ heure personnalisée est obligatoire")
        Boolean heurePersonnalise,

        @Size(max = 255, message = "Le libellé ne peut pas dépasser 255 caractères")
        String libelle,

        @NotNull(message = "L'état d'activation est obligatoire")
        Boolean isActive,

        @NotNull(message = "Le type de demande est obligatoire")
        UUID typeDemandePublicId
) {
}