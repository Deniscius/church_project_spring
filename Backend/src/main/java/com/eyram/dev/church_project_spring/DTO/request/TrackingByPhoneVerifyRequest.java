package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrackingByPhoneVerifyRequest(
        @NotBlank @Size(max = 30) String telephone,
        @NotBlank @Size(min = 4, max = 8) String code
) {
}
