package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.entities.DemandeDate;
import com.eyram.dev.church_project_spring.enums.NatureForfaitEnum;
import com.eyram.dev.church_project_spring.enums.StatutDemandeEnum;
import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;
import com.eyram.dev.church_project_spring.enums.StatutValidationEnum;
import com.eyram.dev.church_project_spring.repositories.DemandeDateRepository;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DemandePaymentEligibilityService {

    private static final String VALIDATION_REQUIRED =
            "Cette demande spéciale doit être validée par la paroisse avant le paiement";
    private static final String CELEBRATION_PASSED =
            "Heure de célébration dépassée — paiement indisponible";

    private final DemandeDateRepository demandeDateRepository;
    private final Clock clock;

    public boolean requiresValidation(Demande demande) {
        return demande != null
                && demande.getForfaitTarif() != null
                && demande.getForfaitTarif().getNatureForfait() == NatureForfaitEnum.SPECIALE;
    }

    public PaymentEligibility evaluate(Demande demande) {
        List<DemandeDate> dates = demande != null && demande.getId() != null
                ? demandeDateRepository.findByDemande_IdAndStatusDelFalse(demande.getId())
                : List.of();
        return evaluate(demande, dates);
    }

    public PaymentEligibility evaluate(Demande demande, List<DemandeDate> dates) {
        boolean validationRequired = requiresValidation(demande);
        if (demande == null || Boolean.TRUE.equals(demande.getStatusDel())) {
            return unavailable(validationRequired, "Cette demande n'est plus disponible", null);
        }
        if (demande.getStatutPaiement() == StatutPaiementEnum.PAYE) {
            return unavailable(validationRequired, "Paiement déjà confirmé", firstCelebrationAt(demande, dates));
        }
        if (demande.getStatutDemande() == StatutDemandeEnum.ANNULEE
                || demande.getStatutDemande() == StatutDemandeEnum.REJETEE
                || demande.getStatutDemande() == StatutDemandeEnum.TERMINEE) {
            return unavailable(validationRequired, "Cette demande n'est pas éligible au paiement",
                    firstCelebrationAt(demande, dates));
        }
        if (validationRequired && demande.getStatutValidation() != StatutValidationEnum.VALIDEE) {
            return unavailable(true, VALIDATION_REQUIRED, firstCelebrationAt(demande, dates));
        }

        LocalDateTime firstCelebrationAt = firstCelebrationAt(demande, dates);
        if (firstCelebrationAt == null) {
            return unavailable(validationRequired,
                    "Aucune heure de célébration valide n'est associée à cette demande", null);
        }
        if (!firstCelebrationAt.isAfter(LocalDateTime.now(clock))) {
            return unavailable(validationRequired, CELEBRATION_PASSED, firstCelebrationAt);
        }
        return new PaymentEligibility(validationRequired, true, null, firstCelebrationAt);
    }

    public void assertCanStartPayment(Demande demande) {
        PaymentEligibility eligibility = evaluate(demande);
        if (!eligibility.paymentAvailable()) {
            throw new BusinessRuleException(eligibility.unavailableReason());
        }
    }

    private PaymentEligibility unavailable(
            boolean validationRequired,
            String reason,
            LocalDateTime firstCelebrationAt
    ) {
        return new PaymentEligibility(validationRequired, false, reason, firstCelebrationAt);
    }

    private LocalDateTime firstCelebrationAt(Demande demande, List<DemandeDate> dates) {
        if (dates == null) {
            return null;
        }
        return dates.stream()
                .filter(row -> row != null && !Boolean.TRUE.equals(row.getStatusDel()))
                .map(row -> celebrationAt(demande, row))
                .filter(java.util.Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    private LocalDateTime celebrationAt(Demande demande, DemandeDate row) {
        if (row.getDateCelebration() == null) {
            return null;
        }
        LocalTime time = row.getHeurePersonnalisee();
        if (time == null && row.getHoraire() != null) {
            time = row.getHoraire().getHeureCelebration();
        }
        if (time == null && demande.getHeurePersonnalisee() != null) {
            time = demande.getHeurePersonnalisee();
        }
        if (time == null && demande.getHoraire() != null) {
            time = demande.getHoraire().getHeureCelebration();
        }
        return time != null ? LocalDateTime.of(row.getDateCelebration(), time) : null;
    }

    public record PaymentEligibility(
            boolean validationRequired,
            boolean paymentAvailable,
            String unavailableReason,
            LocalDateTime firstCelebrationAt
    ) {
    }
}
