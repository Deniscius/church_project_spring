package com.eyram.dev.church_project_spring.DTO.response;

import java.util.UUID;

public record ParoisseResponse(
        UUID publicId,
        String nom,
        String tel,
        Boolean statusDel
) {
}
