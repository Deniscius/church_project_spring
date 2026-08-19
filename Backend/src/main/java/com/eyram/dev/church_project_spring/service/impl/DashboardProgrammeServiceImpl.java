package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.response.UpcomingCelebrationResponse;
import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.entities.DemandeDate;
import com.eyram.dev.church_project_spring.entities.Horaire;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.enums.StatutDemandeEnum;
import com.eyram.dev.church_project_spring.repositories.DemandeDateRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.service.DashboardProgrammeService;
import com.eyram.dev.church_project_spring.utils.FideleNameUtils;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardProgrammeServiceImpl implements DashboardProgrammeService {

    private static final int MAX_WINDOW_DAYS = 31;

    private final ParoisseRepository paroisseRepository;
    private final DemandeDateRepository demandeDateRepository;
    private final TenantAccessService tenantAccessService;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<UpcomingCelebrationResponse> findUpcoming(UUID paroissePublicId, int jours) {
        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));
        tenantAccessService.checkParoisseAccess(paroisse);

        int safeDays = Math.min(Math.max(jours, 1), MAX_WINDOW_DAYS);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate debut = now.toLocalDate();
        LocalDate fin = debut.plusDays(safeDays - 1L);

        return demandeDateRepository.findUpcomingByParoisse(
                        paroisse,
                        debut,
                        fin,
                        EnumSet.of(StatutDemandeEnum.EN_ATTENTE, StatutDemandeEnum.VALIDEE)
                )
                .stream()
                .filter(row -> !celebrationAt(row).isBefore(now))
                .map(this::toResponse)
                .sorted(
                        Comparator.comparing(UpcomingCelebrationResponse::dateCelebration)
                                .thenComparing(
                                        response -> response.heureCelebration() != null
                                                ? response.heureCelebration()
                                                : LocalTime.MAX
                                )
                                .thenComparing(UpcomingCelebrationResponse::codeSuivie)
                )
                .toList();
    }

    private UpcomingCelebrationResponse toResponse(DemandeDate row) {
        Demande demande = row.getDemande();
        Horaire horaire = effectiveHoraire(row, demande);

        return new UpcomingCelebrationResponse(
                demande.getPublicId(),
                row.getPublicId(),
                demande.getCodeSuivie(),
                row.getDateCelebration(),
                effectiveTime(row, demande),
                horaire != null ? horaire.getLibelle() : null,
                demande.getIntention(),
                demande.getTypeDemande() != null ? demande.getTypeDemande().getLibelle() : null,
                FideleNameUtils.format(demande.getPrenomFidele(), demande.getNomFidele()),
                demande.getStatutDemande(),
                demande.getStatutPaiement()
        );
    }

    private LocalDateTime celebrationAt(DemandeDate row) {
        return LocalDateTime.of(row.getDateCelebration(), effectiveTime(row, row.getDemande()));
    }

    private LocalTime effectiveTime(DemandeDate row, Demande demande) {
        if (row.getHeurePersonnalisee() != null) {
            return row.getHeurePersonnalisee();
        }
        if (row.getHoraire() != null && row.getHoraire().getHeureCelebration() != null) {
            return row.getHoraire().getHeureCelebration();
        }
        if (demande != null && demande.getHeurePersonnalisee() != null) {
            return demande.getHeurePersonnalisee();
        }
        if (demande != null && demande.getHoraire() != null && demande.getHoraire().getHeureCelebration() != null) {
            return demande.getHoraire().getHeureCelebration();
        }
        return LocalTime.MIDNIGHT;
    }

    private Horaire effectiveHoraire(DemandeDate row, Demande demande) {
        if (row.getHoraire() != null) {
            return row.getHoraire();
        }
        return demande != null ? demande.getHoraire() : null;
    }
}
