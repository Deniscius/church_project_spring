package com.eyram.dev.church_project_spring.DTO.response;

import java.util.UUID;

public record TypePaiementResponse(
        UUID publicId,
        String libelle,
        Boolean statusDel
) {
}
