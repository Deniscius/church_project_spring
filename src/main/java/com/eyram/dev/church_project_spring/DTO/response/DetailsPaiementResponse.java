package com.eyram.dev.church_project_spring.DTO.response;

import java.time.LocalDate;
import java.util.UUID;

public record DetailsPaiementResponse (
        UUID publicId,
        LocalDate dateDetailsPaiement,
        Double montant,
        Boolean statusDel
) {
}
