package com.eyram.dev.church_project_spring.DTO.response;

import java.math.BigDecimal;
import java.util.List;

public record DemandeParoisseStatsResponse(
        long total,
        long enAttente,
        long validees,
        BigDecimal volumeMontant,
        List<DemandeResponse> recentes,
        /** Demandes non payées dont la 1ère célébration est dans les 3 jours. */
        long impayeesProchesCelebration,
        List<DemandeResponse> impayeesProches
) {
}
