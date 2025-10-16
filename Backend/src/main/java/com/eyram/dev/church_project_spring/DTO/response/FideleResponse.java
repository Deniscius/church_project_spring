package com.eyram.dev.church_project_spring.DTO.response;

import java.util.UUID;

public record FideleResponse(
        UUID publicId,
        String nom,
        String prenoms,
        String tel,
        Boolean status

) {
}
