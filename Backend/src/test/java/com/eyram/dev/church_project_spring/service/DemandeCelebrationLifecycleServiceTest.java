package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.config.DemandePaymentProperties;
import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.entities.DemandeDate;
import com.eyram.dev.church_project_spring.enums.StatutDemandeEnum;
import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;
import com.eyram.dev.church_project_spring.repositories.DemandeDateRepository;
import com.eyram.dev.church_project_spring.repositories.DemandeRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.service.mail.AppMailService;
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
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemandeCelebrationLifecycleServiceTest {

    private static final ZoneId LOME = ZoneId.of("Africa/Lome");

    @Mock
    private DemandeDateRepository demandeDateRepository;
    @Mock
    private DemandeRepository demandeRepository;
    @Mock
    private TenantAccessService tenantAccessService;
    @Mock
    private AppMailService appMailService;

    private DemandePaymentProperties properties;

    @BeforeEach
    void setUp() {
        properties = new DemandePaymentProperties();
        properties.setCelebrationAutoCompleteEnabled(true);
        properties.setCelebrationAutoCompleteDelayHours(5);
    }

    @Test
    void autoComplete_doesNotCompleteBeforeFiveHoursAfterScheduledTime() {
        DemandeDate slot = slotAt(LocalDate.of(2026, 8, 17), LocalTime.of(10, 0));
        when(demandeDateRepository.findPendingCelebrationsInDateWindow(
                any(), any(), any(), anySet()
        )).thenReturn(List.of(slot));

        DemandeCelebrationLifecycleService service = serviceAt("2026-08-17T14:59:59Z");

        int marked = service.autoCompletePastCelebrations();

        assertEquals(0, marked);
        assertFalse(Boolean.TRUE.equals(slot.getCelebre()));
        verify(demandeDateRepository, never()).save(any(DemandeDate.class));
    }

    @Test
    void autoComplete_completesAtFiveHoursAfterScheduledTime() {
        DemandeDate slot = slotAt(LocalDate.of(2026, 8, 17), LocalTime.of(10, 0));
        when(demandeDateRepository.findPendingCelebrationsInDateWindow(
                any(), any(), any(), anySet()
        )).thenReturn(List.of(slot));
        // Une autre date reste en attente : ce test porte uniquement sur le créneau.
        when(demandeDateRepository.countPendingCelebrationsByDemandeId(any())).thenReturn(1L);

        DemandeCelebrationLifecycleService service = serviceAt("2026-08-17T15:00:00Z");

        int marked = service.autoCompletePastCelebrations();

        assertEquals(1, marked);
        assertTrue(Boolean.TRUE.equals(slot.getCelebre()));
        verify(demandeDateRepository).save(slot);
    }

    @Test
    void markCelebrated_rejectsUnpaidDemand() {
        UUID slotId = UUID.randomUUID();
        DemandeDate slot = slotAt(LocalDate.of(2026, 8, 17), LocalTime.of(10, 0));
        slot.setPublicId(slotId);
        slot.getDemande().setStatutPaiement(StatutPaiementEnum.NON_PAYE);
        when(demandeDateRepository.findByPublicIdAndStatusDelFalse(slotId))
                .thenReturn(Optional.of(slot));

        DemandeCelebrationLifecycleService service = serviceAt("2026-08-17T15:00:00Z");

        assertThrows(BusinessRuleException.class, () -> service.markCelebrated(slotId));
        assertFalse(Boolean.TRUE.equals(slot.getCelebre()));
        verify(demandeDateRepository, never()).save(any(DemandeDate.class));
    }

    private DemandeDate slotAt(LocalDate date, LocalTime time) {
        Demande demande = new Demande();
        demande.setStatutDemande(StatutDemandeEnum.VALIDEE);

        DemandeDate slot = new DemandeDate();
        slot.setDateCelebration(date);
        slot.setHeurePersonnalisee(time);
        slot.setCelebre(false);
        slot.setDemande(demande);
        return slot;
    }

    private DemandeCelebrationLifecycleService serviceAt(String instant) {
        Clock clock = Clock.fixed(Instant.parse(instant), LOME);
        return new DemandeCelebrationLifecycleService(
                demandeDateRepository,
                demandeRepository,
                tenantAccessService,
                properties,
                appMailService,
                clock
        );
    }
}
