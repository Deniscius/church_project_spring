package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InscriptionOtpSendRequest(
        @NotBlank @Email @Size(max = 150) String email
) {
}
