package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.entities.DemandeDate;
import com.eyram.dev.church_project_spring.entities.Horaire;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.enums.StatutDemandeEnum;
import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;
import com.eyram.dev.church_project_spring.repositories.DemandeDateRepository;
import com.eyram.dev.church_project_spring.repositories.HoraireRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.service.HoraireService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.cache.CacheManager;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardProgrammeServiceImplTest {

    @Mock private ParoisseRepository paroisseRepository;
    @Mock private DemandeDateRepository demandeDateRepository;
    @Mock private HoraireRepository horaireRepository;
    @Mock private HoraireService horaireService;
    @Mock private TenantAccessService tenantAccessService;
    @Mock private CacheManager cacheManager;

    private Clock clock;
    private DashboardProgrammeServiceImpl service;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-08-19T12:00:00Z"), ZoneOffset.UTC);
        service = new DashboardProgrammeServiceImpl(
                paroisseRepository,
                demandeDateRepository,
                horaireRepository,
                horaireService,
                tenantAccessService,
                clock,
                cacheManager
        );
    }

    @Test
    void findUpcoming_excludesPastSlotsAndReturnsFutureProgramming() {
        UUID paroissePublicId = UUID.randomUUID();
        Paroisse paroisse = parish(paroissePublicId);

        DemandeDate past = row(paroisse, LocalDate.of(2026, 8, 19), LocalTime.of(10, 0), "D-PAST");
        DemandeDate futureToday = row(paroisse, LocalDate.of(2026, 8, 19), LocalTime.of(15, 0), "D-TODAY");
        DemandeDate tomorrow = row(paroisse, LocalDate.of(2026, 8, 20), LocalTime.of(7, 0), "D-TOMORROW");

        when(paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId))
                .thenReturn(Optional.of(paroisse));
        when(demandeDateRepository.findUpcomingByParoisse(
                eq(paroisse),
                eq(LocalDate.of(2026, 8, 19)),
                eq(LocalDate.of(2026, 9, 1)),
                any()
        )).thenReturn(List.of(tomorrow, past, futureToday));

        var result = service.findUpcoming(paroissePublicId, 14);

        assertEquals(2, result.size());
        assertEquals("D-TODAY", result.get(0).codeSuivie());
        assertEquals(LocalTime.of(15, 0), result.get(0).heureCelebration());
        assertTrue(result.get(0).modifiable());
        assertEquals("D-TOMORROW", result.get(1).codeSuivie());
        verify(tenantAccessService).checkParoisseAccess(paroisse);
    }

    @Test
    void findPast_keepsPastDatesIncludingCompletedRequests() {
        UUID paroissePublicId = UUID.randomUUID();
        Paroisse paroisse = parish(paroissePublicId);
        DemandeDate yesterday = row(paroisse, LocalDate.of(2026, 8, 18), LocalTime.of(18, 0), "D-YESTERDAY");
        yesterday.getDemande().setStatutDemande(StatutDemandeEnum.TERMINEE);
        yesterday.setCelebre(true);
        DemandeDate passedToday = row(paroisse, LocalDate.of(2026, 8, 19), LocalTime.of(9, 0), "D-TODAY-PAST");
        DemandeDate futureToday = row(paroisse, LocalDate.of(2026, 8, 19), LocalTime.of(16, 0), "D-TODAY-FUTURE");

        when(paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId))
                .thenReturn(Optional.of(paroisse));
        when(demandeDateRepository.findPastByParoisse(
                eq(paroisse),
                eq(LocalDate.of(2026, 8, 6)),
                eq(LocalDate.of(2026, 8, 19)),
                any()
        )).thenReturn(List.of(yesterday, futureToday, passedToday));

        var result = service.findPast(paroissePublicId, 14);

        assertEquals(2, result.size());
        assertEquals("D-TODAY-PAST", result.get(0).codeSuivie());
        assertEquals("D-YESTERDAY", result.get(1).codeSuivie());
        assertFalse(result.get(0).modifiable());
        assertTrue(result.get(1).celebre());
    }

    @Test
    void updateSchedule_changesOnlySlotOnSameDay() {
        UUID paroissePublicId = UUID.randomUUID();
        Paroisse paroisse = parish(paroissePublicId);
        DemandeDate programmation = row(
                paroisse,
                LocalDate.of(2026, 8, 20),
                LocalTime.of(7, 0),
                "D-CHANGE"
        );
        UUID newHorairePublicId = UUID.randomUUID();
        Horaire newHoraire = new Horaire();
        newHoraire.setPublicId(newHorairePublicId);
        newHoraire.setParoisse(paroisse);
        newHoraire.setIsActive(true);
        newHoraire.setHeureCelebration(LocalTime.of(18, 0));
        newHoraire.setLibelle("Messe du soir");

        when(paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId))
                .thenReturn(Optional.of(paroisse));
        when(demandeDateRepository.findByPublicIdAndStatusDelFalse(programmation.getPublicId()))
                .thenReturn(Optional.of(programmation));
        when(horaireRepository.findByPublicIdAndStatusDelFalse(newHorairePublicId))
                .thenReturn(Optional.of(newHoraire));
        when(demandeDateRepository.save(programmation)).thenReturn(programmation);

        var result = service.updateSchedule(
                paroissePublicId,
                programmation.getPublicId(),
                newHorairePublicId
        );

        assertEquals(LocalDate.of(2026, 8, 20), result.dateCelebration());
        assertEquals(LocalTime.of(18, 0), result.heureCelebration());
        assertEquals(newHorairePublicId, result.horairePublicId());
        assertEquals(newHoraire, programmation.getHoraire());
        assertEquals(newHoraire, programmation.getDemande().getHoraire());
        verify(horaireService).assertHoraireAllowedOnDate(newHoraire, LocalDate.of(2026, 8, 20));
        verify(horaireService).assertUniqueMassSlot(
                paroissePublicId,
                LocalDate.of(2026, 8, 20),
                newHoraire,
                null
        );
    }

    private Paroisse parish(UUID publicId) {
        Paroisse paroisse = new Paroisse();
        paroisse.setPublicId(publicId);
        return paroisse;
    }

    private DemandeDate row(Paroisse paroisse, LocalDate date, LocalTime time, String code) {
        Horaire horaire = new Horaire();
        horaire.setPublicId(UUID.randomUUID());
        horaire.setParoisse(paroisse);
        horaire.setIsActive(true);
        horaire.setHeureCelebration(time);
        horaire.setLibelle("Messe");

        Demande demande = new Demande();
        demande.setPublicId(UUID.randomUUID());
        demande.setCodeSuivie(code);
        demande.setIntention("Intention " + code);
        demande.setNomFidele("KOFFI");
        demande.setPrenomFidele("Eyram");
        demande.setStatutDemande(StatutDemandeEnum.VALIDEE);
        demande.setStatutPaiement(StatutPaiementEnum.PAYE);
        demande.setHoraire(horaire);
        demande.setParoisse(paroisse);

        DemandeDate row = new DemandeDate();
        row.setPublicId(UUID.randomUUID());
        row.setDateCelebration(date);
        row.setOrdre(1);
        row.setHoraire(horaire);
        row.setCelebre(false);
        row.setDemande(demande);
        row.setStatusDel(false);
        return row;
    }
}
