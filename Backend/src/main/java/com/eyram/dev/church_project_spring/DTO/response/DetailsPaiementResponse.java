package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.ModePaiement;
import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;

import java.time.LocalDateTime;
import java.util.UUID;

public record DetailsPaiementResponse(
        UUID publicId,
        LocalDateTime dateDetailsPaiement,
        Integer montant,
        String idTransaction,
        StatutPaiementEnum statutPaiement,
        ModePaiement modePaiement,
        String numero,
        UUID typePaiementPublicId,
        String typePaiementLibelle,
        UUID facturePublicId,
        String refFacture
) {
}