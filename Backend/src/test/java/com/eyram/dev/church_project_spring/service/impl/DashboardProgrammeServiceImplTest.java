package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.entities.DemandeDate;
import com.eyram.dev.church_project_spring.entities.Horaire;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.enums.StatutDemandeEnum;
import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;
import com.eyram.dev.church_project_spring.repositories.DemandeDateRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardProgrammeServiceImplTest {

    @Mock private ParoisseRepository paroisseRepository;
    @Mock private DemandeDateRepository demandeDateRepository;
    @Mock private TenantAccessService tenantAccessService;

    private Clock clock;
    private DashboardProgrammeServiceImpl service;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-08-19T12:00:00Z"), ZoneOffset.UTC);
        service = new DashboardProgrammeServiceImpl(
                paroisseRepository,
                demandeDateRepository,
                tenantAccessService,
                clock
        );
    }

    @Test
    void findUpcoming_excludesPastSlotsAndReturnsFutureProgramming() {
        UUID paroissePublicId = UUID.randomUUID();
        Paroisse paroisse = new Paroisse();
        paroisse.setPublicId(paroissePublicId);

        DemandeDate past = row(LocalDate.of(2026, 8, 19), LocalTime.of(10, 0), "D-PAST");
        DemandeDate futureToday = row(LocalDate.of(2026, 8, 19), LocalTime.of(15, 0), "D-TODAY");
        DemandeDate tomorrow = row(LocalDate.of(2026, 8, 20), LocalTime.of(7, 0), "D-TOMORROW");

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
        assertEquals("D-TOMORROW", result.get(1).codeSuivie());
        verify(tenantAccessService).checkParoisseAccess(paroisse);
    }

    private DemandeDate row(LocalDate date, LocalTime time, String code) {
        Horaire horaire = new Horaire();
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
