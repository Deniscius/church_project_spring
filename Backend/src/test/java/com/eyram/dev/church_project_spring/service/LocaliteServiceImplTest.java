package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.LocaliteRequest;
import com.eyram.dev.church_project_spring.DTO.response.LocaliteResponse;
import com.eyram.dev.church_project_spring.entities.Localite;
import com.eyram.dev.church_project_spring.mappers.LocaliteMapper;
import com.eyram.dev.church_project_spring.repositories.LocaliteRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.service.impl.LocaliteServiceImpl;
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
class LocaliteServiceImplTest {

    @Mock
    private LocaliteRepository localiteRepository;

    @Mock
    private ParoisseRepository paroisseRepository;

    @Mock
    private LocaliteMapper localiteMapper;

    @InjectMocks
    private LocaliteServiceImpl service;

    @Test
    void createsNormalizedLocality() {
        LocaliteRequest request = new LocaliteRequest("  Lomé  ", "  Grand   Marché ");
        Localite entity = new Localite();
        LocaliteResponse expected = response(UUID.randomUUID(), "Lomé", "Grand Marché");
        when(localiteMapper.dtoToModel(new LocaliteRequest("Lomé", "Grand Marché"))).thenReturn(entity);
        when(localiteRepository.save(entity)).thenReturn(entity);
        when(localiteMapper.modelToDto(entity)).thenReturn(expected);

        LocaliteResponse result = service.create(request);

        assertEquals(expected, result);
        assertFalse(entity.getStatusDel());
        verify(localiteRepository)
                .existsByVilleIgnoreCaseAndQuartierIgnoreCaseAndStatusDelFalse("Lomé", "Grand Marché");
    }

    @Test
    void rejectsDuplicateOnCreation() {
        LocaliteRequest request = new LocaliteRequest("Lomé", "Tokoin");
        when(localiteRepository.existsByVilleIgnoreCaseAndQuartierIgnoreCaseAndStatusDelFalse("Lomé", "Tokoin"))
                .thenReturn(true);

        assertThrows(AlreadyExistException.class, () -> service.create(request));

        verify(localiteRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validatesLengthAfterWhitespaceNormalization() {
        LocaliteRequest request = new LocaliteRequest(" A ", "Tokoin");

        assertThrows(IllegalArgumentException.class, () -> service.create(request));

        verify(localiteRepository, never())
                .existsByVilleIgnoreCaseAndQuartierIgnoreCaseAndStatusDelFalse(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()
                );
    }

    @Test
    void rejectsDuplicateOnUpdateExcludingCurrentRow() {
        UUID publicId = UUID.randomUUID();
        Localite localite = localite(publicId);
        when(localiteRepository.findByPublicIdAndStatusDelFalse(publicId)).thenReturn(Optional.of(localite));
        when(localiteRepository.existsByVilleIgnoreCaseAndQuartierIgnoreCaseAndStatusDelFalseAndPublicIdNot(
                "Lomé", "Tokoin", publicId)).thenReturn(true);

        assertThrows(
                AlreadyExistException.class,
                () -> service.update(publicId, new LocaliteRequest("Lomé", "Tokoin"))
        );

        verify(localiteMapper, never()).updateEntityFromDto(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updatesEditableFields() {
        UUID publicId = UUID.randomUUID();
        Localite localite = localite(publicId);
        LocaliteRequest normalized = new LocaliteRequest("Lomé", "Adidogomé");
        LocaliteResponse expected = response(publicId, "Lomé", "Adidogomé");
        when(localiteRepository.findByPublicIdAndStatusDelFalse(publicId)).thenReturn(Optional.of(localite));
        when(localiteRepository.save(localite)).thenReturn(localite);
        when(localiteMapper.modelToDto(localite)).thenReturn(expected);

        LocaliteResponse result = service.update(
                publicId,
                new LocaliteRequest(" Lomé ", " Adidogomé ")
        );

        assertEquals(expected, result);
        verify(localiteMapper).updateEntityFromDto(normalized, localite);
    }

    @Test
    void refusesDeletionWhenParishUsesLocality() {
        UUID publicId = UUID.randomUUID();
        Localite localite = localite(publicId);
        when(localiteRepository.findByPublicIdAndStatusDelFalse(publicId)).thenReturn(Optional.of(localite));
        when(paroisseRepository.existsByLocaliteAndStatusDelFalse(localite)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> service.deleteByPublicId(publicId));

        assertFalse(localite.getStatusDel());
        verify(localiteRepository, never()).save(localite);
    }

    @Test
    void softDeletesUnusedLocality() {
        UUID publicId = UUID.randomUUID();
        Localite localite = localite(publicId);
        when(localiteRepository.findByPublicIdAndStatusDelFalse(publicId)).thenReturn(Optional.of(localite));

        service.deleteByPublicId(publicId);

        assertTrue(localite.getStatusDel());
        verify(localiteRepository).save(localite);
    }

    @Test
    void returnsSortedActiveLocalitiesFromRepository() {
        Localite first = localite(UUID.randomUUID());
        Localite second = localite(UUID.randomUUID());
        LocaliteResponse firstResponse = response(first.getPublicId(), "Kara", "Centre");
        LocaliteResponse secondResponse = response(second.getPublicId(), "Lomé", "Tokoin");
        when(localiteRepository.findAllByStatusDelFalseOrderByVilleAscQuartierAsc())
                .thenReturn(List.of(first, second));
        when(localiteMapper.modelToDto(first)).thenReturn(firstResponse);
        when(localiteMapper.modelToDto(second)).thenReturn(secondResponse);

        assertEquals(List.of(firstResponse, secondResponse), service.getAll());
    }

    @Test
    void reportsMissingLocality() {
        UUID publicId = UUID.randomUUID();
        when(localiteRepository.findByPublicIdAndStatusDelFalse(publicId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getByPublicId(publicId));
    }

    private Localite localite(UUID publicId) {
        Localite localite = new Localite();
        localite.setPublicId(publicId);
        localite.setVille("Lomé");
        localite.setQuartier("Tokoin");
        localite.setStatusDel(false);
        return localite;
    }

    private LocaliteResponse response(UUID publicId, String ville, String quartier) {
        return new LocaliteResponse(publicId, ville, quartier, null, null);
    }
}
