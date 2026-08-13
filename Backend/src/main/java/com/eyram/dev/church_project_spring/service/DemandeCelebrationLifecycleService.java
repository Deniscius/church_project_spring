package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.response.CelebrationIntentionResponse;
import com.eyram.dev.church_project_spring.config.DemandePaymentProperties;
import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.entities.DemandeDate;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.enums.StatutDemandeEnum;
import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;
import com.eyram.dev.church_project_spring.repositories.DemandeDateRepository;
import com.eyram.dev.church_project_spring.repositories.DemandeRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.service.mail.AppMailService;
import com.eyram.dev.church_project_spring.utils.FideleNameUtils;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Rappels avant messe (J-1 / H-2) et passage en « célébrée »
 * (manuel sur la feuille, ou auto dès que l'heure est passée).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemandeCelebrationLifecycleService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH' h 'mm");

    private final DemandeDateRepository demandeDateRepository;
    private final DemandeRepository demandeRepository;
    private final TenantAccessService tenantAccessService;
    private final DemandePaymentProperties properties;
    private final AppMailService appMailService;
    private final Clock clock;

    @Transactional
    public CelebrationIntentionResponse markCelebrated(UUID demandeDatePublicId) {
        DemandeDate slot = demandeDateRepository.findByPublicIdAndStatusDelFalse(demandeDatePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Date de célébration introuvable"));
        Demande demande = slot.getDemande();
        if (demande == null || Boolean.TRUE.equals(demande.getStatusDel())) {
            throw new ResourceNotFoundException("Demande introuvable");
        }
        tenantAccessService.checkParoisseAccess(demande.getParoisse());

        if (demande.getStatutDemande() == StatutDemandeEnum.ANNULEE
                || demande.getStatutDemande() == StatutDemandeEnum.REJETEE) {
            throw new BusinessRuleException("Cette demande ne peut plus être marquée célébrée.");
        }
        if (demande.getStatutDemande() != StatutDemandeEnum.VALIDEE
                && demande.getStatutDemande() != StatutDemandeEnum.TERMINEE) {
            throw new BusinessRuleException("Seules les demandes validées peuvent être marquées célébrées.");
        }

        if (!Boolean.TRUE.equals(slot.getCelebre())) {
            applyCelebrated(slot, LocalDateTime.now(clock));
        }
        return toLightResponse(slot);
    }

    /**
     * Marque célébrées toutes les dates dont l'heure de messe est passée.
     * @return nombre de créneaux passés en célébré
     */
    @Transactional
    public int autoCompletePastCelebrations() {
        if (!properties.isCelebrationAutoCompleteEnabled()) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        List<DemandeDate> candidates = demandeDateRepository.findPendingCelebrationsInDateWindow(
                now.toLocalDate().minusDays(2),
                now.toLocalDate(),
                StatutPaiementEnum.PAYE,
                EnumSet.of(StatutDemandeEnum.VALIDEE, StatutDemandeEnum.TERMINEE)
        );

        int marked = 0;
        for (DemandeDate slot : candidates) {
            LocalDateTime celebrationAt = resolveCelebrationAt(slot);
            if (!celebrationAt.isAfter(now) && !Boolean.TRUE.equals(slot.getCelebre())) {
                applyCelebrated(slot, now);
                marked++;
            }
        }
        return marked;
    }

    /**
     * Rappels J-1 (fidèle + paroisse) et H-2 (fidèle) pour intentions payées.
     * @return nombre d'e-mails fidèles envoyés
     */
    @Transactional
    public int sendUpcomingCelebrationReminders() {
        if (!properties.isCelebrationReminderEnabled()) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        List<DemandeDate> candidates = demandeDateRepository.findPendingCelebrationsInDateWindow(
                now.toLocalDate(),
                now.toLocalDate().plusDays(2),
                StatutPaiementEnum.PAYE,
                EnumSet.of(StatutDemandeEnum.VALIDEE)
        );

        int sent = 0;
        for (DemandeDate slot : candidates) {
            Demande demande = slot.getDemande();
            if (demande == null || Boolean.TRUE.equals(demande.getStatusDel())) {
                continue;
            }
            LocalDateTime celebrationAt = resolveCelebrationAt(slot);
            if (celebrationAt.isBefore(now)) {
                continue;
            }

            long hoursUntil = java.time.Duration.between(now, celebrationAt).toHours();
            long minutesUntil = java.time.Duration.between(now, celebrationAt).toMinutes();

            // H-2 : dans les 2 prochaines heures (et > 0).
            if (minutesUntil > 0 && minutesUntil <= 120 && slot.getLastReminderH2At() == null) {
                if (sendFideleReminder(demande, celebrationAt, true)) {
                    slot.setLastReminderH2At(now);
                    demandeDateRepository.save(slot);
                    sent++;
                }
                continue;
            }

            // J-1 : entre ~12 h et 36 h avant la messe (une seule fois).
            if (hoursUntil >= 12 && hoursUntil <= 36 && slot.getLastReminderJ1At() == null) {
                boolean fideleOk = sendFideleReminder(demande, celebrationAt, false);
                sendParishReminder(demande, celebrationAt);
                if (fideleOk) {
                    sent++;
                }
                slot.setLastReminderJ1At(now);
                demandeDateRepository.save(slot);
            }
        }
        return sent;
    }

    private void applyCelebrated(DemandeDate slot, LocalDateTime at) {
        slot.setCelebre(true);
        slot.setCelebreAt(at);
        demandeDateRepository.save(slot);

        Demande demande = slot.getDemande();
        long pending = demandeDateRepository.countPendingCelebrationsByDemandeId(demande.getId());
        if (pending == 0 && demande.getStatutDemande() == StatutDemandeEnum.VALIDEE) {
            demande.setStatutDemande(StatutDemandeEnum.TERMINEE);
            demandeRepository.save(demande);
            log.info("Demande {} → TERMINEE (toutes les célébrations confirmées)", demande.getCodeSuivie());
        }
    }

    static LocalDateTime resolveCelebrationAt(DemandeDate slot) {
        Demande demande = slot.getDemande();
        LocalTime time = null;
        if (slot.getHeurePersonnalisee() != null) {
            time = slot.getHeurePersonnalisee();
        } else if (slot.getHoraire() != null && slot.getHoraire().getHeureCelebration() != null) {
            time = slot.getHoraire().getHeureCelebration();
        } else if (demande != null && demande.getHeurePersonnalisee() != null) {
            time = demande.getHeurePersonnalisee();
        } else if (demande != null && demande.getHoraire() != null) {
            time = demande.getHoraire().getHeureCelebration();
        }
        if (time == null) {
            time = LocalTime.MIDNIGHT;
        }
        return LocalDateTime.of(slot.getDateCelebration(), time.withSecond(0).withNano(0));
    }

    private boolean sendFideleReminder(Demande demande, LocalDateTime celebrationAt, boolean h2) {
        if (!StringUtils.hasText(demande.getEmailFidele())) {
            return false;
        }
        String base = publicBase();
        String suiviUrl = base + "/suivi/resultat?code=" + demande.getCodeSuivie();
        String paroisseNom = demande.getParoisse() != null ? demande.getParoisse().getNom() : "votre paroisse";
        String fidel = FideleNameUtils.format(demande.getPrenomFidele(), demande.getNomFidele());
        String when = celebrationAt.toLocalDate().format(DATE_FORMAT)
                + " à " + celebrationAt.toLocalTime().format(TIME_FORMAT);

        String subject = h2
                ? "Rappel : votre intention est célébrée bientôt — " + demande.getCodeSuivie()
                : "Rappel : votre intention demain — " + demande.getCodeSuivie();
        String timing = h2
                ? "dans moins de 2 heures"
                : "demain (ou dans moins de 36 heures)";
        String body = """
                Bonjour %s,

                Votre intention de messe à %s sera célébrée %s.
                Code de suivi : %s
                Date / heure : %s
                Intention : %s

                Suivre la demande : %s

                — Messes Archidiocèse de Lomé
                """.formatted(
                fidel,
                paroisseNom,
                timing,
                demande.getCodeSuivie(),
                when,
                demande.getIntention() != null ? demande.getIntention() : "—",
                suiviUrl
        );
        appMailService.sendText(demande.getEmailFidele().trim(), subject, body);
        log.info("Rappel célébration {} envoyé pour {} → {}",
                h2 ? "H-2" : "J-1", demande.getCodeSuivie(), demande.getEmailFidele());
        return true;
    }

    private void sendParishReminder(Demande demande, LocalDateTime celebrationAt) {
        Paroisse paroisse = demande.getParoisse();
        if (paroisse == null || !StringUtils.hasText(paroisse.getEmail())) {
            return;
        }
        String when = celebrationAt.toLocalDate().format(DATE_FORMAT)
                + " à " + celebrationAt.toLocalTime().format(TIME_FORMAT);
        String subject = "Rappel feuille d'intentions — " + when;
        String body = """
                Bonjour,

                Une intention payée approche à %s.
                Code : %s
                Fidèle : %s
                Intention : %s
                Célébration : %s

                Pensez à préparer / imprimer la feuille d'intentions.

                — Missanye
                """.formatted(
                paroisse.getNom(),
                demande.getCodeSuivie(),
                FideleNameUtils.format(demande.getPrenomFidele(), demande.getNomFidele()),
                demande.getIntention() != null ? demande.getIntention() : "—",
                when
        );
        appMailService.sendText(paroisse.getEmail().trim(), subject, body);
    }

    private String publicBase() {
        return StringUtils.hasText(properties.getPublicBaseUrl())
                ? properties.getPublicBaseUrl().replaceAll("/+$", "")
                : "http://localhost:5173";
    }

    private CelebrationIntentionResponse toLightResponse(DemandeDate slot) {
        Demande demande = slot.getDemande();
        return new CelebrationIntentionResponse(
                slot.getPublicId(),
                demande.getPublicId(),
                demande.getCodeSuivie(),
                slot.getDateCelebration(),
                demande.getCreatedAt(),
                slot.getOrdre(),
                null,
                null,
                null,
                resolveCelebrationAt(slot).toLocalTime(),
                null,
                demande.getIntention(),
                null,
                null,
                null,
                demande.getNomFidele(),
                demande.getPrenomFidele(),
                demande.getTelFidele(),
                demande.getEmailFidele(),
                demande.getStatutDemande() != null ? demande.getStatutDemande().name() : null,
                demande.getStatutPaiement() != null ? demande.getStatutPaiement().name() : null,
                Boolean.TRUE.equals(slot.getCelebre()),
                slot.getCelebreAt()
        );
    }
}
