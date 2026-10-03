package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.AnneePastoraleRequest;
import com.eyram.dev.church_project_spring.DTO.response.AnneePastoraleResponse;
import com.eyram.dev.church_project_spring.entities.AnneePastorale;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.enums.StatutAnneePastorale;
import com.eyram.dev.church_project_spring.mappers.AnneePastoraleMapper;
import com.eyram.dev.church_project_spring.repositories.AnneePastoraleRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.utils.exception.AlreadyExistException;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnneePastoraleServiceImplTest {

    @Mock private AnneePastoraleRepository repository;
    @Mock private ParoisseRepository paroisseRepository;
    @Mock private AnneePastoraleMapper mapper;
    @Mock private TenantAccessService tenantAccessService;

    private AnneePastoraleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AnneePastoraleServiceImpl(
                repository,
                paroisseRepository,
                mapper,
                tenantAccessService
        );
    }

    @Test
    void createNormalizesContentAndStartsAsDraft() {
        UUID paroisseId = UUID.randomUUID();
        Paroisse paroisse = parish(paroisseId);
        AnneePastoraleRequest request = request(paroisseId, null, "  2026  -  2027  ");
        AnneePastorale entity = new AnneePastorale();
        AnneePastoraleResponse response = response(StatutAnneePastorale.BROUILLON);

        when(paroisseRepository.findByPublicIdForUpdate(paroisseId))
                .thenReturn(Optional.of(paroisse));
        when(repository.existsByParoisseAndLibelleIgnoreCaseAndStatusDelFalse(
                paroisse, "2026 - 2027")).thenReturn(false);
        when(repository.existsOverlappingPeriod(
                paroisse, request.dateDebut(), request.dateFin(), null)).thenReturn(false);
        when(mapper.dtoToModel(request)).thenReturn(entity);
        when(repository.saveAndFlush(entity)).thenReturn(entity);
        when(mapper.modelToDto(entity)).thenReturn(response);

        AnneePastoraleResponse result = service.create(request);

        verify(tenantAccessService).checkParoisseAccess(paroisse);
        assertEquals("2026 - 2027", entity.getLibelle());
        assertEquals("Programme pastoral", entity.getDescription());
        assertEquals(StatutAnneePastorale.BROUILLON, entity.getStatut());
        assertFalse(entity.getStatusDel());
        assertSame(response, result);
    }

    @Test
    void createRejectsAnOverlappingPeriod() {
        UUID paroisseId = UUID.randomUUID();
        Paroisse paroisse = parish(paroisseId);
        AnneePastoraleRequest request = request(paroisseId, null, "2026-2027");

        when(paroisseRepository.findByPublicIdForUpdate(paroisseId))
                .thenReturn(Optional.of(paroisse));
        when(repository.existsOverlappingPeriod(
                paroisse, request.dateDebut(), request.dateFin(), null)).thenReturn(true);

        BusinessRuleException error = assertThrows(
                BusinessRuleException.class,
                () -> service.create(request)
        );

        assertTrue(error.getMessage().contains("chevauche"));
        verify(repository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateRejectsAStaleClientVersion() {
        UUID publicId = UUID.randomUUID();
        UUID paroisseId = UUID.randomUUID();
        Paroisse paroisse = parish(paroisseId);
        AnneePastorale entity = pastoralYear(publicId, paroisse, StatutAnneePastorale.BROUILLON, 4L);
        AnneePastoraleRequest request = request(paroisseId, 3L, "2026-2027");

        when(repository.findByPublicIdAndStatusDelFalse(publicId)).thenReturn(Optional.of(entity));

        BusinessRuleException error = assertThrows(
                BusinessRuleException.class,
                () -> service.update(publicId, request)
        );

        assertTrue(error.getMessage().contains("autre utilisateur"));
        verify(mapper, never()).updateEntityFromDto(request, entity);
    }

    @Test
    void publishTransitionsDraftAndPersistsTimestamp() {
        UUID publicId = UUID.randomUUID();
        Paroisse paroisse = parish(UUID.randomUUID());
        AnneePastorale entity = pastoralYear(publicId, paroisse, StatutAnneePastorale.BROUILLON, 0L);
        AnneePastoraleResponse response = response(StatutAnneePastorale.PUBLIEE);

        when(repository.findByPublicIdAndStatusDelFalse(publicId)).thenReturn(Optional.of(entity));
        when(repository.findByParoisseAndStatutAndStatusDelFalse(
                paroisse, StatutAnneePastorale.PUBLIEE)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(entity)).thenReturn(entity);
        when(mapper.modelToDto(entity)).thenReturn(response);

        AnneePastoraleResponse result = service.publish(publicId, 0L);

        assertEquals(StatutAnneePastorale.PUBLIEE, entity.getStatut());
        assertNotNull(entity.getPublishedAt());
        assertSame(response, result);
    }

    @Test
    void publishRejectsASecondPublishedYear() {
        UUID publicId = UUID.randomUUID();
        Paroisse paroisse = parish(UUID.randomUUID());
        AnneePastorale entity = pastoralYear(publicId, paroisse, StatutAnneePastorale.BROUILLON, 0L);
        AnneePastorale other = pastoralYear(
                UUID.randomUUID(), paroisse, StatutAnneePastorale.PUBLIEE, 2L
        );

        when(repository.findByPublicIdAndStatusDelFalse(publicId)).thenReturn(Optional.of(entity));
        when(repository.findByParoisseAndStatutAndStatusDelFalse(
                paroisse, StatutAnneePastorale.PUBLIEE)).thenReturn(Optional.of(other));

        assertThrows(AlreadyExistException.class, () -> service.publish(publicId, 0L));
        verify(repository, never()).saveAndFlush(entity);
    }

    @Test
    void closeTransitionsPublishedYear() {
        UUID publicId = UUID.randomUUID();
        Paroisse paroisse = parish(UUID.randomUUID());
        AnneePastorale entity = pastoralYear(publicId, paroisse, StatutAnneePastorale.PUBLIEE, 1L);
        AnneePastoraleResponse response = response(StatutAnneePastorale.CLOTUREE);

        when(repository.findByPublicIdAndStatusDelFalse(publicId)).thenReturn(Optional.of(entity));
        when(repository.saveAndFlush(entity)).thenReturn(entity);
        when(mapper.modelToDto(entity)).thenReturn(response);

        service.close(publicId, 1L);

        assertEquals(StatutAnneePastorale.CLOTUREE, entity.getStatut());
        assertNotNull(entity.getClosedAt());
    }

    @Test
    void deletePreservesPublishedHistory() {
        UUID publicId = UUID.randomUUID();
        Paroisse paroisse = parish(UUID.randomUUID());
        AnneePastorale entity = pastoralYear(publicId, paroisse, StatutAnneePastorale.PUBLIEE, 1L);

        when(repository.findByPublicIdAndStatusDelFalse(publicId)).thenReturn(Optional.of(entity));

        assertThrows(BusinessRuleException.class, () -> service.deleteByPublicId(publicId, 1L));
        verify(repository, never()).save(entity);
    }

    private AnneePastoraleRequest request(UUID paroisseId, Long version, String label) {
        return new AnneePastoraleRequest(
                label,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2027, 8, 31),
                "  Programme   pastoral  ",
                paroisseId,
                version
        );
    }

    private Paroisse parish(UUID publicId) {
        Paroisse paroisse = new Paroisse();
        paroisse.setPublicId(publicId);
        paroisse.setStatusDel(false);
        return paroisse;
    }

    private AnneePastorale pastoralYear(
            UUID publicId,
            Paroisse paroisse,
            StatutAnneePastorale statut,
            Long version
    ) {
        AnneePastorale entity = new AnneePastorale();
        entity.setPublicId(publicId);
        entity.setParoisse(paroisse);
        entity.setStatut(statut);
        entity.setVersion(version);
        entity.setStatusDel(false);
        return entity;
    }

    private AnneePastoraleResponse response(StatutAnneePastorale statut) {
        return new AnneePastoraleResponse(
                UUID.randomUUID(),
                "2026-2027",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2027, 8, 31),
                null,
                statut,
                null,
                null,
                0L,
                UUID.randomUUID(),
                "Paroisse test",
                null,
                null
        );
    }
}
