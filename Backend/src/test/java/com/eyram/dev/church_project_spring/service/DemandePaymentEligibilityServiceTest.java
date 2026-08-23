package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.entities.DemandeDate;
import com.eyram.dev.church_project_spring.entities.ForfaitTarif;
import com.eyram.dev.church_project_spring.entities.Horaire;
import com.eyram.dev.church_project_spring.enums.NatureForfaitEnum;
import com.eyram.dev.church_project_spring.enums.StatutDemandeEnum;
import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;
import com.eyram.dev.church_project_spring.enums.StatutValidationEnum;
import com.eyram.dev.church_project_spring.repositories.DemandeDateRepository;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemandePaymentEligibilityServiceTest {

    @Mock private DemandeDateRepository demandeDateRepository;

    private DemandePaymentEligibilityService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-23T10:00:00Z"), ZoneOffset.UTC);
        service = new DemandePaymentEligibilityService(demandeDateRepository, clock);
    }

    @Test
    void normalRequestDoesNotRequireManualValidation() {
        Demande demande = request(NatureForfaitEnum.NORMALE, StatutValidationEnum.EN_ATTENTE);
        when(demandeDateRepository.findByDemande_IdAndStatusDelFalse(1L))
                .thenReturn(List.of(slot(demande, LocalDate.of(2026, 8, 24), LocalTime.of(8, 0))));

        var result = service.evaluate(demande);

        assertFalse(result.validationRequired());
        assertTrue(result.paymentAvailable());
    }

    @Test
    void unvalidatedSpecialRequestCannotBePaid() {
        Demande demande = request(NatureForfaitEnum.SPECIALE, StatutValidationEnum.EN_ATTENTE);
        when(demandeDateRepository.findByDemande_IdAndStatusDelFalse(1L))
                .thenReturn(List.of(slot(demande, LocalDate.of(2026, 8, 24), LocalTime.of(8, 0))));

        assertThrows(BusinessRuleException.class, () -> service.assertCanStartPayment(demande));
    }

    @Test
    void validatedSpecialRequestCanBePaidBeforeCelebration() {
        Demande demande = request(NatureForfaitEnum.SPECIALE, StatutValidationEnum.VALIDEE);
        when(demandeDateRepository.findByDemande_IdAndStatusDelFalse(1L))
                .thenReturn(List.of(slot(demande, LocalDate.of(2026, 8, 24), LocalTime.of(8, 0))));

        assertTrue(service.evaluate(demande).paymentAvailable());
    }

    @Test
    void requestCannotBePaidAtOrAfterFirstCelebration() {
        Demande demande = request(NatureForfaitEnum.NORMALE, StatutValidationEnum.VALIDEE);
        when(demandeDateRepository.findByDemande_IdAndStatusDelFalse(1L))
                .thenReturn(List.of(slot(demande, LocalDate.of(2026, 8, 23), LocalTime.of(10, 0))));

        assertFalse(service.evaluate(demande).paymentAvailable());
        assertThrows(BusinessRuleException.class, () -> service.assertCanStartPayment(demande));
    }

    private Demande request(NatureForfaitEnum nature, StatutValidationEnum validation) {
        ForfaitTarif forfait = new ForfaitTarif();
        forfait.setNatureForfait(nature);

        Demande demande = new Demande();
        demande.setId(1L);
        demande.setForfaitTarif(forfait);
        demande.setStatutValidation(validation);
        demande.setStatutDemande(StatutDemandeEnum.VALIDEE);
        demande.setStatutPaiement(StatutPaiementEnum.NON_PAYE);
        demande.setStatusDel(false);
        return demande;
    }

    private DemandeDate slot(Demande demande, LocalDate date, LocalTime time) {
        Horaire horaire = new Horaire();
        horaire.setHeureCelebration(time);

        DemandeDate row = new DemandeDate();
        row.setDemande(demande);
        row.setDateCelebration(date);
        row.setHoraire(horaire);
        row.setStatusDel(false);
        return row;
    }
}
