package com.eyram.dev.church_project_spring.DTO.request;

import com.eyram.dev.church_project_spring.enums.ModePaiement;
import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record DetailsPaiementRequest(
        @NotNull(message = "La date du paiement est obligatoire")
        LocalDateTime dateDetailsPaiement,

        @NotNull(message = "Le montant est obligatoire")
        Integer montant,

        String idTransaction,

        @NotNull(message = "Le statut du paiement est obligatoire")
        StatutPaiementEnum statutPaiement,

        @NotBlank(message = "Le numéro est obligatoire")
        String numero,

        @NotNull(message = "Le type de paiement est obligatoire")
        UUID typePaiementPublicId,

        @NotNull(message = "La facture est obligatoire")
        UUID facturePublicId
) {
}