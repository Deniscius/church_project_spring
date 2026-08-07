package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReversementDecisionRequest(
        @Size(max = 120) String referenceVirement,
        @Size(max = 500) String motif
) {
}
