package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.StructurePastoraleRequest;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.StructurePastorale;
import com.eyram.dev.church_project_spring.enums.TypeStructurePastorale;
import com.eyram.dev.church_project_spring.mappers.StructurePastoraleMapper;
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

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StructurePastoraleServiceImplTest {

    @Mock private StructurePastoraleRepository repository;
    @Mock private MandatPastoralRepository mandatRepository;
    @Mock private ParoisseRepository paroisseRepository;
    @Mock private StructurePastoraleMapper mapper;
    @Mock private TenantAccessService tenantAccessService;

    private StructurePastoraleServiceImpl service;
    private Paroisse paroisse;

    @BeforeEach
    void setUp() {
        service = new StructurePastoraleServiceImpl(
                repository,
                mandatRepository,
                paroisseRepository,
                mapper,
                tenantAccessService
        );
        paroisse = new Paroisse();
        paroisse.setPublicId(UUID.randomUUID());
    }

    @Test
    void updateRejectsParishMove() {
        StructurePastorale entity = new StructurePastorale();
        entity.setPublicId(UUID.randomUUID());
        entity.setParoisse(paroisse);
        entity.setVersion(2L);
        StructurePastoraleRequest request = new StructurePastoraleRequest(
                TypeStructurePastorale.CPP,
                "Bureau exécutif du CPP",
                null,
                0,
                UUID.randomUUID(),
                2L
        );
        when(repository.findByPublicIdAndStatusDelFalse(entity.getPublicId()))
                .thenReturn(Optional.of(entity));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.update(entity.getPublicId(), request)
        );

        assertEquals("Une structure pastorale ne peut pas changer de paroisse", exception.getMessage());
    }

    @Test
    void archiveRejectsStructureWithMandateHistory() {
        StructurePastorale entity = new StructurePastorale();
        entity.setPublicId(UUID.randomUUID());
        entity.setParoisse(paroisse);
        entity.setVersion(1L);
        entity.setActif(false);
        when(repository.findByPublicIdAndStatusDelFalse(entity.getPublicId()))
                .thenReturn(Optional.of(entity));
        when(mandatRepository.existsByStructurePastoraleAndStatusDelFalse(entity))
                .thenReturn(true);

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.archive(entity.getPublicId(), 1L)
        );

        assertEquals(
                "Cette structure possède un historique de mandats et doit être conservée",
                exception.getMessage()
        );
    }
}
