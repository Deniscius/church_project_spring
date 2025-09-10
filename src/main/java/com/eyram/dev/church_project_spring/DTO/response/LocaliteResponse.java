package com.eyram.dev.church_project_spring.DTO.response;

import java.util.UUID;

public record LocaliteResponse(
        UUID publicId,
        String ville,
        String quartier,
        Boolean statusDel
) {
}
