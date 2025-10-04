package com.eyram.dev.church_project_spring.DTO.response;

import java.util.UUID;

public record TypeDemandeResponse(
        UUID publicId,
        String libelle,
        String description,
        Boolean statusDel
) {}
