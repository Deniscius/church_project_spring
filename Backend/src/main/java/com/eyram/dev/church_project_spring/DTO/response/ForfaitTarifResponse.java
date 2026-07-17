package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.JourSemaine;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record ForfaitTarifResponse(
        UUID publicId,
        String codeForfait,
        String nomForfait,
        BigDecimal montantForfait,
        Integer nombreJour,
        Integer nombreCelebration,
        Set<JourSemaine> joursCelebrationAutorises,
        Boolean heurePersonnalise,
        String libelle,
        Boolean isActive,
        UUID typeDemandePublicId,
        String typeDemandeLibelle,
        Boolean statusDel,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}