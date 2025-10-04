package com.eyram.dev.church_project_spring.DTO.request;

import java.util.UUID;

public record ParoisseRequest(
        UUID publicId,
        String nom,
        String tel
) {
}
