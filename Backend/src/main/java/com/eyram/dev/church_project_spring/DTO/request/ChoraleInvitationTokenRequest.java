package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChoraleInvitationTokenRequest(
        @NotBlank(message = "L'invitation est obligatoire")
        @Size(min = 40, max = 200, message = "L'invitation est invalide")
        String token
) {}
