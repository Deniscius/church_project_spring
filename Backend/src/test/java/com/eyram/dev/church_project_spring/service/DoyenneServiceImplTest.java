package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.DoyenneRequest;
import com.eyram.dev.church_project_spring.DTO.response.DoyenneResponse;
import com.eyram.dev.church_project_spring.entities.Doyenne;
import com.eyram.dev.church_project_spring.mappers.DoyenneMapper;
import com.eyram.dev.church_project_spring.repositories.DoyenneRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.service.impl.DoyenneServiceImpl;
import com.eyram.dev.church_project_spring.utils.exception.AlreadyExistException;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoyenneServiceImplTest {

    @Mock
    private DoyenneRepository doyenneRepository;

    @Mock
    private ParoisseRepository paroisseRepository;

    @Mock
    private DoyenneMapper doyenneMapper;

    @InjectMocks
    private DoyenneServiceImpl service;

    @Test
    void createsNormalizedDeanery() {
        DoyenneRequest request = new DoyenneRequest("  Doyenné de Lomé-Centre  ", "  Zone   pastorale ");
        Doyenne entity = new Doyenne();
        DoyenneResponse expected = response(UUID.randomUUID(), "Doyenné de Lomé-Centre", "Zone pastorale");
        when(doyenneMapper.dtoToModel(new DoyenneRequest("Doyenné de Lomé-Centre", "Zone pastorale"))).thenReturn(entity);
        when(doyenneRepository.save(entity)).thenReturn(entity);
        when(doyenneMapper.modelToDto(entity)).thenReturn(expected);

        DoyenneResponse result = service.create(request);

        assertEquals(expected, result);
        assertFalse(entity.getStatusDel());
        verify(doyenneRepository)
                .existsByNomIgnoreCaseAndStatusDelFalse("Doyenné de Lomé-Centre");
    }

    @Test
    void rejectsDuplicateOnCreation() {
        DoyenneRequest request = new DoyenneRequest("Doyenné de Lomé-Centre", null);
        when(doyenneRepository.existsByNomIgnoreCaseAndStatusDelFalse("Doyenné de Lomé-Centre"))
                .thenReturn(true);

        assertThrows(AlreadyExistException.class, () -> service.create(request));

        verify(doyenneRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validatesLengthAfterWhitespaceNormalization() {
        DoyenneRequest request = new DoyenneRequest(" A ", null);

        assertThrows(IllegalArgumentException.class, () -> service.create(request));

        verify(doyenneRepository, never())
                .existsByNomIgnoreCaseAndStatusDelFalse(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsDuplicateOnUpdateExcludingCurrentRow() {
        UUID publicId = UUID.randomUUID();
        Doyenne doyenne = doyenne(publicId);
        when(doyenneRepository.findByPublicIdAndStatusDelFalse(publicId)).thenReturn(Optional.of(doyenne));
        when(doyenneRepository.existsByNomIgnoreCaseAndStatusDelFalseAndPublicIdNot(
                "Doyenné de Lomé-Centre", publicId)).thenReturn(true);

        assertThrows(
                AlreadyExistException.class,
                () -> service.update(publicId, new DoyenneRequest("Doyenné de Lomé-Centre", null))
        );

        verify(doyenneMapper, never()).updateEntityFromDto(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updatesEditableFields() {
        UUID publicId = UUID.randomUUID();
        Doyenne doyenne = doyenne(publicId);
        DoyenneRequest normalized = new DoyenneRequest("Doyenné de Lomé-Ouest", "Adidogomé");
        DoyenneResponse expected = response(publicId, "Doyenné de Lomé-Ouest", "Adidogomé");
        when(doyenneRepository.findByPublicIdAndStatusDelFalse(publicId)).thenReturn(Optional.of(doyenne));
        when(doyenneRepository.save(doyenne)).thenReturn(doyenne);
        when(doyenneMapper.modelToDto(doyenne)).thenReturn(expected);

        DoyenneResponse result = service.update(
                publicId,
                new DoyenneRequest(" Doyenné de Lomé-Ouest ", " Adidogomé ")
        );

        assertEquals(expected, result);
        verify(doyenneMapper).updateEntityFromDto(normalized, doyenne);
    }

    @Test
    void refusesDeletionWhenParishUsesDeanery() {
        UUID publicId = UUID.randomUUID();
        Doyenne doyenne = doyenne(publicId);
        when(doyenneRepository.findByPublicIdAndStatusDelFalse(publicId)).thenReturn(Optional.of(doyenne));
        when(paroisseRepository.existsByDoyenneAndStatusDelFalse(doyenne)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> service.deleteByPublicId(publicId));

        assertFalse(doyenne.getStatusDel());
        verify(doyenneRepository, never()).save(doyenne);
    }

    @Test
    void softDeletesUnusedDeanery() {
        UUID publicId = UUID.randomUUID();
        Doyenne doyenne = doyenne(publicId);
        when(doyenneRepository.findByPublicIdAndStatusDelFalse(publicId)).thenReturn(Optional.of(doyenne));

        service.deleteByPublicId(publicId);

        assertTrue(doyenne.getStatusDel());
        verify(doyenneRepository).save(doyenne);
    }

    @Test
    void returnsSortedActiveDeaneriesFromRepository() {
        Doyenne first = doyenne(UUID.randomUUID());
        Doyenne second = doyenne(UUID.randomUUID());
        DoyenneResponse firstResponse = response(first.getPublicId(), "Doyenné de Kara", null);
        DoyenneResponse secondResponse = response(second.getPublicId(), "Doyenné de Lomé", null);
        when(doyenneRepository.findAllByStatusDelFalseOrderByNomAsc())
                .thenReturn(List.of(first, second));
        when(doyenneMapper.modelToDto(first)).thenReturn(firstResponse);
        when(doyenneMapper.modelToDto(second)).thenReturn(secondResponse);

        assertEquals(List.of(firstResponse, secondResponse), service.getAll());
    }

    @Test
    void reportsMissingDeanery() {
        UUID publicId = UUID.randomUUID();
        when(doyenneRepository.findByPublicIdAndStatusDelFalse(publicId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getByPublicId(publicId));
    }

    private Doyenne doyenne(UUID publicId) {
        Doyenne doyenne = new Doyenne();
        doyenne.setPublicId(publicId);
        doyenne.setNom("Doyenné de Lomé-Centre");
        doyenne.setDescription("Zone pastorale");
        doyenne.setStatusDel(false);
        return doyenne;
    }

    private DoyenneResponse response(UUID publicId, String nom, String description) {
        return new DoyenneResponse(publicId, nom, description, null, null);
    }
}
