package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.ModePaiement;

import java.util.UUID;

public record TypePaiementResponse(
        UUID publicId,
        String libelle,
        ModePaiement mode
) {
}