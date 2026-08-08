package com.eyram.dev.church_project_spring.DTO.response;

import java.util.List;

public record CaisseResumeResponse(
        int total,
        int totalDuJour,
        int totalDuMois,
        int nombreEncaissements,
        List<CaisseEncaissementResponse> encaissements
) {
}
