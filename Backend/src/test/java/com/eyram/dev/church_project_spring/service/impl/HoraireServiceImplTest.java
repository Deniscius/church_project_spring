package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.response.ProgrammeJourResponse;
import com.eyram.dev.church_project_spring.config.ApplicationTimeConfig;
import com.eyram.dev.church_project_spring.entities.Horaire;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.enums.JourSemaine;
import com.eyram.dev.church_project_spring.enums.ModeProgrammeJour;
import com.eyram.dev.church_project_spring.mappers.HoraireMapper;
import com.eyram.dev.church_project_spring.repositories.HoraireRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HoraireServiceImplTest {

    @Mock private HoraireRepository horaireRepository;
    @Mock private ParoisseRepository paroisseRepository;
    @Mock private HoraireMapper horaireMapper;
    @Mock private TenantAccessService tenantAccessService;

    private HoraireServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HoraireServiceImpl(
                horaireRepository,
                paroisseRepository,
                horaireMapper,
                tenantAccessService
        );
    }

    @Test
    void getProgramme_august30FollowsSundayDefaultsWhenNoDateExceptionExists() {
        UUID paroissePublicId = UUID.randomUUID();
        Paroisse paroisse = parish(paroissePublicId);
        LocalDate august30 = LocalDate.of(2026, 8, 30);
        Horaire seven = weekly(JourSemaine.DIMANCHE, LocalTime.of(7, 0));
        Horaire nine = weekly(JourSemaine.DIMANCHE, LocalTime.of(9, 0));

        when(paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId))
                .thenReturn(Optional.of(paroisse));
        when(horaireRepository.findByParoisseAndIsActiveTrueAndStatusDelFalseAndDateSpecifiqueIsNull(paroisse))
                .thenReturn(List.of(seven, nine));
        when(horaireRepository.findByParoisseAndIsActiveTrueAndStatusDelFalseAndDateSpecifiqueBetween(
                paroisse, august30, august30
        )).thenReturn(List.of());

        ProgrammeJourResponse result = service.getProgramme(paroissePublicId, august30, august30).get(0);

        assertEquals(ModeProgrammeJour.HEBDOMADAIRE, result.modeProgramme());
        assertEquals(List.of(LocalTime.of(7, 0), LocalTime.of(9, 0)),
                result.creneaux().stream().map(ProgrammeJourResponse.Creneau::heureCelebration).toList());
    }

    @Test
    void getProgramme_customDateOverrideReplacesWeeklySlots() {
        UUID paroissePublicId = UUID.randomUUID();
        Paroisse paroisse = parish(paroissePublicId);
        LocalDate date = LocalDate.of(2026, 8, 30);
        Horaire weekly = weekly(JourSemaine.DIMANCHE, LocalTime.of(7, 0));
        Horaire custom = dated(date, LocalTime.of(8, 30), false, true);

        when(paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId))
                .thenReturn(Optional.of(paroisse));
        when(horaireRepository.findByParoisseAndIsActiveTrueAndStatusDelFalseAndDateSpecifiqueIsNull(paroisse))
                .thenReturn(List.of(weekly));
        when(horaireRepository.findByParoisseAndIsActiveTrueAndStatusDelFalseAndDateSpecifiqueBetween(
                paroisse, date, date
        )).thenReturn(List.of(custom));

        ProgrammeJourResponse result = service.getProgramme(paroissePublicId, date, date).get(0);

        assertEquals(ModeProgrammeJour.PERSONNALISE, result.modeProgramme());
        assertEquals(1, result.creneaux().size());
        assertEquals(LocalTime.of(8, 30), result.creneaux().get(0).heureCelebration());
        assertTrue(result.creneaux().get(0).programmeJourOverride());
    }

    @Test
    void resetProgrammeForDate_removesDateExceptionsAndRestoresWeeklyGrid() {
        UUID paroissePublicId = UUID.randomUUID();
        Paroisse paroisse = parish(paroissePublicId);
        LocalDate date = LocalDate.now(ApplicationTimeConfig.BUSINESS_ZONE).plusDays(10);
        JourSemaine day = JourSemaine.fromDayOfWeek(date.getDayOfWeek());
        Horaire weekly = weekly(day, LocalTime.of(6, 30));
        Horaire accidentalUnique = dated(date, LocalTime.of(10, 0), true, false);

        when(paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId))
                .thenReturn(Optional.of(paroisse));
        when(horaireRepository.findByParoisseAndDateSpecifiqueAndStatusDelFalse(paroisse, date))
                .thenReturn(List.of(accidentalUnique));
        when(horaireRepository.findByParoisseAndIsActiveTrueAndStatusDelFalseAndDateSpecifiqueIsNull(paroisse))
                .thenReturn(List.of(weekly));

        ProgrammeJourResponse result = service.resetProgrammeForDate(paroissePublicId, date);

        verify(tenantAccessService).checkCatalogWriteAccess(paroisse);
        verify(horaireRepository).saveAll(List.of(accidentalUnique));
        verify(horaireRepository).flush();
        assertFalse(accidentalUnique.getIsActive());
        assertTrue(accidentalUnique.getStatusDel());
        assertEquals(ModeProgrammeJour.HEBDOMADAIRE, result.modeProgramme());
        assertEquals(LocalTime.of(6, 30), result.creneaux().get(0).heureCelebration());
    }

    private Paroisse parish(UUID publicId) {
        Paroisse paroisse = new Paroisse();
        paroisse.setPublicId(publicId);
        return paroisse;
    }

    private Horaire weekly(JourSemaine day, LocalTime time) {
        Horaire horaire = new Horaire();
        horaire.setPublicId(UUID.randomUUID());
        horaire.setJourSemaine(day);
        horaire.setHeureCelebration(time);
        horaire.setIsActive(true);
        horaire.setStatusDel(false);
        horaire.setProgrammeJourOverride(false);
        return horaire;
    }

    private Horaire dated(LocalDate date, LocalTime time, boolean unique, boolean override) {
        Horaire horaire = weekly(JourSemaine.fromDayOfWeek(date.getDayOfWeek()), time);
        horaire.setDateSpecifique(date);
        horaire.setUniqueSurParoisse(unique);
        horaire.setProgrammeJourOverride(override);
        return horaire;
    }
}
