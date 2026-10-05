package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.ActeurPastoralRequest;
import com.eyram.dev.church_project_spring.DTO.response.ActeurPastoralResponse;
import com.eyram.dev.church_project_spring.entities.ActeurPastoral;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.enums.CategorieActeurPastoral;
import com.eyram.dev.church_project_spring.mappers.ActeurPastoralMapper;
import com.eyram.dev.church_project_spring.repositories.ActeurPastoralRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActeurPastoralServiceImplTest {

    @Mock private ActeurPastoralRepository repository;
    @Mock private ParoisseRepository paroisseRepository;
    @Mock private ActeurPastoralMapper mapper;
    @Mock private TenantAccessService tenantAccessService;

    private ActeurPastoralServiceImpl service;
    private Paroisse paroisse;

    @BeforeEach
    void setUp() {
        service = new ActeurPastoralServiceImpl(
                repository,
                paroisseRepository,
                mapper,
                tenantAccessService
        );
        paroisse = new Paroisse();
        paroisse.setPublicId(UUID.randomUUID());
        paroisse.setNom("Saint-Pierre");
    }

    @Test
    void createNormalizesIdentityAndContactData() {
        ActeurPastoralRequest request = new ActeurPastoralRequest(
                "  ATTISSO  ",
                " Michel  Kossi ",
                " Père ",
                CategorieActeurPastoral.CLERGE,
                " 90 00 00 00 ",
                " MICHEL@EXAMPLE.TG ",
                " Curé de la paroisse ",
                paroisse.getPublicId(),
                null
        );
        ActeurPastoral entity = new ActeurPastoral();
        ActeurPastoralResponse response = responseFor(entity);

        when(paroisseRepository.findByPublicIdAndStatusDelFalse(paroisse.getPublicId()))
                .thenReturn(Optional.of(paroisse));
        when(mapper.dtoToModel(request)).thenReturn(entity);
        when(repository.saveAndFlush(entity)).thenReturn(entity);
        when(mapper.modelToDto(entity)).thenReturn(response);

        assertSame(response, service.create(request));
        assertEquals("ATTISSO", entity.getNom());
        assertEquals("Michel Kossi", entity.getPrenoms());
        assertEquals("michel@example.tg", entity.getEmail());
        assertEquals(Boolean.TRUE, entity.getActif());
        verify(tenantAccessService).checkParoisseAccess(paroisse);
    }

    @Test
    void updateRejectsParishMove() {
        ActeurPastoral entity = actorWithVersion(3L, true);
        ActeurPastoralRequest request = new ActeurPastoralRequest(
                "ATTISSO",
                "Michel",
                "Père",
                CategorieActeurPastoral.CLERGE,
                null,
                null,
                null,
                UUID.randomUUID(),
                3L
        );
        when(repository.findByPublicIdAndStatusDelFalse(entity.getPublicId()))
                .thenReturn(Optional.of(entity));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.update(entity.getPublicId(), request)
        );

        assertEquals(
                "Un acteur pastoral ne peut pas être déplacé vers une autre paroisse",
                exception.getMessage()
        );
    }

    @Test
    void archiveRequiresInactiveActor() {
        ActeurPastoral entity = actorWithVersion(1L, true);
        when(repository.findByPublicIdAndStatusDelFalse(entity.getPublicId()))
                .thenReturn(Optional.of(entity));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.archive(entity.getPublicId(), 1L)
        );

        assertEquals("Désactivez l'acteur pastoral avant de l'archiver", exception.getMessage());
    }

    @Test
    void searchNormalizesQueryAndKeepsTenantScope() {
        when(paroisseRepository.findByPublicIdAndStatusDelFalse(paroisse.getPublicId()))
                .thenReturn(Optional.of(paroisse));
        when(repository.searchByParoisse(paroisse, true, "%michel%"))
                .thenReturn(List.of());

        assertEquals(0, service.getByParoisse(paroisse.getPublicId(), true, " Michel ").size());
        verify(tenantAccessService).checkParoisseAccess(paroisse);
        verify(repository).searchByParoisse(paroisse, true, "%michel%");
    }

    @Test
    void statusChangeRejectsStaleVersion() {
        ActeurPastoral entity = actorWithVersion(4L, true);
        when(repository.findByPublicIdAndStatusDelFalse(entity.getPublicId()))
                .thenReturn(Optional.of(entity));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.setActif(entity.getPublicId(), false, 3L)
        );

        assertEquals(
                "Cet acteur pastoral a été modifié par un autre utilisateur ; rechargez les données",
                exception.getMessage()
        );
    }

    private ActeurPastoral actorWithVersion(Long version, boolean actif) {
        ActeurPastoral entity = new ActeurPastoral();
        entity.setPublicId(UUID.randomUUID());
        entity.setParoisse(paroisse);
        entity.setVersion(version);
        entity.setActif(actif);
        return entity;
    }

    private ActeurPastoralResponse responseFor(ActeurPastoral entity) {
        return new ActeurPastoralResponse(
                entity.getPublicId(),
                entity.getNom(),
                entity.getPrenoms(),
                entity.getAppellation(),
                entity.getCategorie(),
                entity.getTelephone(),
                entity.getEmail(),
                entity.getNotes(),
                entity.getActif(),
                entity.getVersion(),
                paroisse.getPublicId(),
                paroisse.getNom(),
                null,
                null
        );
    }
}
