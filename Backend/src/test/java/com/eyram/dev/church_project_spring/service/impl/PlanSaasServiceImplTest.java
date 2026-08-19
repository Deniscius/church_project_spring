package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.PlanSaasCreateRequest;
import com.eyram.dev.church_project_spring.DTO.request.PlanSaasRequest;
import com.eyram.dev.church_project_spring.DTO.response.PlanSaasResponse;
import com.eyram.dev.church_project_spring.entities.PlanSaas;
import com.eyram.dev.church_project_spring.mappers.PlanSaasMapper;
import com.eyram.dev.church_project_spring.repositories.PlanSaasRepository;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanSaasServiceImplTest {

    @Mock private PlanSaasRepository repository;
    @Mock private PlanSaasMapper mapper;

    private PlanSaasServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PlanSaasServiceImpl(repository, mapper);
    }

    @Test
    void create_normalizesCodeAndKeepsCommercialValuesDynamic() {
        PlanSaasCreateRequest request = new PlanSaasCreateRequest(
                "trimestriel",
                "Trimestriel",
                "Une formule de trois mois",
                15_000,
                3,
                true,
                false,
                25
        );
        PlanSaas entity = new PlanSaas();
        entity.setActif(true);
        entity.setFeatured(false);
        entity.setMontantXof(15_000);
        entity.setDureeMois(3);
        entity.setOrdreAffichage(25);

        PlanSaasResponse response = new PlanSaasResponse(
                null, "TRIMESTRIEL", "Trimestriel", "Une formule de trois mois",
                15_000, 3, true, false, 25, null, null
        );

        when(repository.existsByCodeIgnoreCaseAndStatusDelFalse("TRIMESTRIEL")).thenReturn(false);
        when(mapper.createEntityFromDto(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.modelToDto(entity)).thenReturn(response);

        PlanSaasResponse saved = service.create(request);

        assertEquals("TRIMESTRIEL", entity.getCode());
        assertEquals("Trimestriel", entity.getNom());
        assertFalse(entity.getStatusDel());
        assertSame(response, saved);
    }

    @Test
    void update_refusesToDisableLastActivePlan() {
        UUID publicId = UUID.randomUUID();
        PlanSaas entity = new PlanSaas();
        entity.setActif(true);

        PlanSaasRequest request = new PlanSaasRequest(
                "Mensuel",
                "Formule mensuelle",
                5_000,
                1,
                false,
                false,
                10
        );

        when(repository.findByPublicIdAndStatusDelFalse(publicId)).thenReturn(Optional.of(entity));
        when(repository.countByActifTrueAndStatusDelFalse()).thenReturn(1L);

        BusinessRuleException error = assertThrows(
                BusinessRuleException.class,
                () -> service.update(publicId, request)
        );

        assertEquals(
                "Au moins une formule SaaS doit rester active pour les nouvelles souscriptions",
                error.getMessage()
        );
    }

    @Test
    void requireDefaultActive_prefersFeaturedPlan() {
        PlanSaas first = new PlanSaas();
        first.setCode("MENSUEL");
        first.setFeatured(false);

        PlanSaas featured = new PlanSaas();
        featured.setCode("ANNUEL");
        featured.setFeatured(true);

        when(repository.findByActifTrueAndStatusDelFalseOrderByOrdreAffichageAscNomAsc())
                .thenReturn(List.of(first, featured));

        assertSame(featured, service.requireDefaultActive());
    }
}
