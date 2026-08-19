package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.response.UpcomingCelebrationResponse;
import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.entities.DemandeDate;
import com.eyram.dev.church_project_spring.entities.Horaire;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.enums.StatutDemandeEnum;
import com.eyram.dev.church_project_spring.repositories.DemandeDateRepository;
import com.eyram.dev.church_project_spring.repositories.HoraireRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.service.DashboardProgrammeService;
import com.eyram.dev.church_project_spring.service.HoraireService;
import com.eyram.dev.church_project_spring.utils.FideleNameUtils;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
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

    private static final int MAX_UPCOMING_WINDOW_DAYS = 31;
    private static final int MAX_PAST_WINDOW_DAYS = 90;

    private final ParoisseRepository paroisseRepository;
    private final DemandeDateRepository demandeDateRepository;
    private final HoraireRepository horaireRepository;
    private final HoraireService horaireService;
    private final TenantAccessService tenantAccessService;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<UpcomingCelebrationResponse> findUpcoming(UUID paroissePublicId, int jours) {
        Paroisse paroisse = requireParoisse(paroissePublicId);
        int safeDays = Math.min(Math.max(jours, 1), MAX_UPCOMING_WINDOW_DAYS);
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
                .map(row -> toResponse(row, now))
                .sorted(futureComparator())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UpcomingCelebrationResponse> findPast(UUID paroissePublicId, int jours) {
        Paroisse paroisse = requireParoisse(paroissePublicId);
        int safeDays = Math.min(Math.max(jours, 1), MAX_PAST_WINDOW_DAYS);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate fin = now.toLocalDate();
        LocalDate debut = fin.minusDays(safeDays - 1L);

        return demandeDateRepository.findPastByParoisse(
                        paroisse,
                        debut,
                        fin,
                        readableStatuses()
                )
                .stream()
                .filter(row -> celebrationAt(row).isBefore(now))
                .map(row -> toResponse(row, now))
                .sorted(pastComparator())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UpcomingCelebrationResponse> findByDate(UUID paroissePublicId, LocalDate date) {
        if (date == null) {
            throw new BusinessRuleException("La date du programme est obligatoire");
        }
        Paroisse paroisse = requireParoisse(paroissePublicId);
        LocalDateTime now = LocalDateTime.now(clock);

        return demandeDateRepository.findProgrammeByParoisseAndDate(
                        paroisse,
                        date,
                        readableStatuses()
                )
                .stream()
                .map(row -> toResponse(row, now))
                .sorted(futureComparator())
                .toList();
    }

    @Override
    @Transactional
    public UpcomingCelebrationResponse updateSchedule(
            UUID paroissePublicId,
            UUID demandeDatePublicId,
            UUID horairePublicId
    ) {
        Paroisse paroisse = requireParoisse(paroissePublicId);
        if (demandeDatePublicId == null) {
            throw new BusinessRuleException("La programmation à modifier est obligatoire");
        }
        if (horairePublicId == null) {
            throw new BusinessRuleException("Le nouveau créneau est obligatoire");
        }

        DemandeDate row = demandeDateRepository.findByPublicIdAndStatusDelFalse(demandeDatePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Programmation introuvable"));
        Demande demande = row.getDemande();
        if (demande == null || demande.getParoisse() == null) {
            throw new ResourceNotFoundException("Demande associée introuvable");
        }
        tenantAccessService.checkParoisseAccess(demande.getParoisse());
        if (!paroisse.getPublicId().equals(demande.getParoisse().getPublicId())) {
            throw new BusinessRuleException("Cette programmation n'appartient pas à la paroisse sélectionnée");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (Boolean.TRUE.equals(row.getCelebre())) {
            throw new BusinessRuleException("Une célébration déjà effectuée ne peut plus être reprogrammée");
        }
        if (!isEditableStatus(demande.getStatutDemande())) {
            throw new BusinessRuleException("Cette demande n'est plus modifiable");
        }
        if (row.getDateCelebration() == null || row.getDateCelebration().isBefore(now.toLocalDate())) {
            throw new BusinessRuleException("Une date déjà passée ne peut plus être modifiée");
        }

        Horaire horaire = horaireRepository.findByPublicIdAndStatusDelFalse(horairePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Créneau introuvable"));
        if (!Boolean.TRUE.equals(horaire.getIsActive())) {
            throw new BusinessRuleException("Ce créneau n'est plus actif");
        }
        if (horaire.getParoisse() == null
                || !paroisse.getPublicId().equals(horaire.getParoisse().getPublicId())) {
            throw new BusinessRuleException("Le créneau ne correspond pas à cette paroisse");
        }

        horaireService.assertHoraireAllowedOnDate(horaire, row.getDateCelebration());
        horaireService.assertUniqueMassSlot(paroissePublicId, row.getDateCelebration(), horaire, null);

        LocalDateTime target = LocalDateTime.of(row.getDateCelebration(), horaire.getHeureCelebration());
        if (!target.isAfter(now)) {
            throw new BusinessRuleException("Le nouveau créneau doit être dans le futur");
        }

        row.setHoraire(horaire);
        row.setHeurePersonnalisee(null);

        // Compatibilité avec les vues historiques qui utilisent encore le créneau
        // principal porté par Demande pour la première date.
        if (Integer.valueOf(1).equals(row.getOrdre())) {
            demande.setHoraire(horaire);
            demande.setHeurePersonnalisee(null);
        }

        DemandeDate saved = demandeDateRepository.save(row);
        return toResponse(saved, now);
    }

    private Paroisse requireParoisse(UUID paroissePublicId) {
        if (paroissePublicId == null) {
            throw new BusinessRuleException("La paroisse est obligatoire");
        }
        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));
        tenantAccessService.checkParoisseAccess(paroisse);
        return paroisse;
    }

    private UpcomingCelebrationResponse toResponse(DemandeDate row, LocalDateTime now) {
        Demande demande = row.getDemande();
        Horaire horaire = effectiveHoraire(row, demande);

        return new UpcomingCelebrationResponse(
                demande.getPublicId(),
                row.getPublicId(),
                demande.getCodeSuivie(),
                row.getDateCelebration(),
                effectiveTime(row, demande),
                horaire != null ? horaire.getPublicId() : null,
                horaire != null ? horaire.getLibelle() : null,
                demande.getIntention(),
                demande.getTypeDemande() != null ? demande.getTypeDemande().getLibelle() : null,
                FideleNameUtils.format(demande.getPrenomFidele(), demande.getNomFidele()),
                demande.getStatutDemande(),
                demande.getStatutPaiement(),
                Boolean.TRUE.equals(row.getCelebre()),
                row.getCelebreAt(),
                isModifiable(row, now)
        );
    }

    private boolean isModifiable(DemandeDate row, LocalDateTime now) {
        Demande demande = row.getDemande();
        return !Boolean.TRUE.equals(row.getCelebre())
                && demande != null
                && isEditableStatus(demande.getStatutDemande())
                && celebrationAt(row).isAfter(now);
    }

    private boolean isEditableStatus(StatutDemandeEnum statut) {
        return statut == StatutDemandeEnum.EN_ATTENTE || statut == StatutDemandeEnum.VALIDEE;
    }

    private EnumSet<StatutDemandeEnum> readableStatuses() {
        return EnumSet.of(
                StatutDemandeEnum.EN_ATTENTE,
                StatutDemandeEnum.VALIDEE,
                StatutDemandeEnum.TERMINEE
        );
    }

    private Comparator<UpcomingCelebrationResponse> futureComparator() {
        return Comparator.comparing(UpcomingCelebrationResponse::dateCelebration)
                .thenComparing(
                        response -> response.heureCelebration() != null
                                ? response.heureCelebration()
                                : LocalTime.MAX
                )
                .thenComparing(UpcomingCelebrationResponse::codeSuivie);
    }

    private Comparator<UpcomingCelebrationResponse> pastComparator() {
        return Comparator.comparing(UpcomingCelebrationResponse::dateCelebration).reversed()
                .thenComparing(
                        UpcomingCelebrationResponse::heureCelebration,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
                .thenComparing(UpcomingCelebrationResponse::codeSuivie);
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
