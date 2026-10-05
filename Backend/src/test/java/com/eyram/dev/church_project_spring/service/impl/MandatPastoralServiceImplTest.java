package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.MandatPastoralRequest;
import com.eyram.dev.church_project_spring.entities.ActeurPastoral;
import com.eyram.dev.church_project_spring.entities.AnneePastorale;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.StructurePastorale;
import com.eyram.dev.church_project_spring.enums.StatutAnneePastorale;
import com.eyram.dev.church_project_spring.mappers.MandatPastoralMapper;
import com.eyram.dev.church_project_spring.repositories.ActeurPastoralRepository;
import com.eyram.dev.church_project_spring.repositories.AnneePastoraleRepository;
import com.eyram.dev.church_project_spring.repositories.MandatPastoralRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.repositories.StructurePastoraleRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MandatPastoralServiceImplTest {

    @Mock private MandatPastoralRepository mandatRepository;
    @Mock private ParoisseRepository paroisseRepository;
    @Mock private AnneePastoraleRepository anneeRepository;
    @Mock private ActeurPastoralRepository acteurRepository;
    @Mock private StructurePastoraleRepository structureRepository;
    @Mock private MandatPastoralMapper mapper;
    @Mock private TenantAccessService tenantAccessService;

    private MandatPastoralServiceImpl service;
    private Paroisse paroisse;
    private AnneePastorale annee;
    private ActeurPastoral acteur;
    private StructurePastorale structure;

    @BeforeEach
    void setUp() {
        service = new MandatPastoralServiceImpl(
                mandatRepository,
                paroisseRepository,
                anneeRepository,
                acteurRepository,
                structureRepository,
                mapper,
                tenantAccessService
        );
        paroisse = new Paroisse();
        paroisse.setPublicId(UUID.randomUUID());

        annee = new AnneePastorale();
        annee.setPublicId(UUID.randomUUID());
        annee.setParoisse(paroisse);
        annee.setDateDebut(LocalDate.of(2026, 9, 1));
        annee.setDateFin(LocalDate.of(2027, 8, 31));
        annee.setStatut(StatutAnneePastorale.PUBLIEE);

        acteur = new ActeurPastoral();
        acteur.setPublicId(UUID.randomUUID());
        acteur.setParoisse(paroisse);
        acteur.setActif(true);

        structure = new StructurePastorale();
        structure.setPublicId(UUID.randomUUID());
        structure.setParoisse(paroisse);
        structure.setActif(true);
    }

    @Test
    void closedPastoralYearIsReadOnly() {
        annee.setStatut(StatutAnneePastorale.CLOTUREE);
        stubContext();
        MandatPastoralRequest request = request(null, null);

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.create(request)
        );

        assertEquals(
                "Une année pastorale clôturée conserve son organisation en lecture seule",
                exception.getMessage()
        );
    }

    @Test
    void mandatePeriodMustStayInsidePastoralYear() {
        stubContext();
        MandatPastoralRequest request = request(
                LocalDate.of(2026, 8, 31),
                LocalDate.of(2027, 8, 31)
        );

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.create(request)
        );

        assertEquals(
                "La période du mandat doit être comprise dans l'année pastorale",
                exception.getMessage()
        );
    }

    private void stubContext() {
        when(paroisseRepository.findByPublicIdAndStatusDelFalse(paroisse.getPublicId()))
                .thenReturn(Optional.of(paroisse));
        when(anneeRepository.findByPublicIdAndStatusDelFalse(annee.getPublicId()))
                .thenReturn(Optional.of(annee));
        when(acteurRepository.findByPublicIdAndStatusDelFalse(acteur.getPublicId()))
                .thenReturn(Optional.of(acteur));
        when(structureRepository.findByPublicIdAndStatusDelFalse(structure.getPublicId()))
                .thenReturn(Optional.of(structure));
    }

    private MandatPastoralRequest request(LocalDate debut, LocalDate fin) {
        return new MandatPastoralRequest(
                "Vice-président",
                "Collaborer à la pastorale générale",
                debut,
                fin,
                1,
                paroisse.getPublicId(),
                annee.getPublicId(),
                acteur.getPublicId(),
                structure.getPublicId(),
                null
        );
    }
}
