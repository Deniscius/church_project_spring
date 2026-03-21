package com.eyram.dev.church_project_spring.DTO.request;

import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record FactureRequest(
        String refFacture,

        LocalDateTime datePaiement,

        @NotNull(message = "Le statut de paiement est obligatoire")
        StatutPaiementEnum statutPaiement,

        @NotNull(message = "La demande est obligatoire")
        UUID demandePublicId
) {
}