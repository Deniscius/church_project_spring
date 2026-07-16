package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseRequest;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseResponse;
import com.eyram.dev.church_project_spring.entities.Localite;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.ParoisseAccess;
import com.eyram.dev.church_project_spring.mappers.ParoisseMapper;
import com.eyram.dev.church_project_spring.repositories.LocaliteRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseAccessRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.service.impl.ParoisseServiceImpl;
import com.eyram.dev.church_project_spring.utils.exception.AlreadyExistException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParoisseServiceImplTest {

    @Mock private ParoisseRepository paroisseRepository;
    @Mock private ParoisseAccessRepository paroisseAccessRepository;
    @Mock private LocaliteRepository localiteRepository;
    @Mock private ParoisseMapper paroisseMapper;
    @Mock private TenantAccessService tenantAccessService;
    @InjectMocks private ParoisseServiceImpl service;

    @Test
    void createsNormalizedParishInAnActiveLocality() {
        UUID localiteId = UUID.randomUUID();
        Localite localite = localite(localiteId);
        Paroisse entity = new Paroisse();
        ParoisseRequest normalized = new ParoisseRequest(
                "Saint Jean", "12 rue de la Paix", "contact@example.com", null, localiteId
        );
        ParoisseResponse expected = response(UUID.randomUUID(), localiteId);
        when(tenantAccessService.isGlobalUser()).thenReturn(true);
        when(localiteRepository.findByPublicIdAndStatusDelFalse(localiteId)).thenReturn(Optional.of(localite));
        when(paroisseMapper.dtoToModel(normalized)).thenReturn(entity);
        when(paroisseRepository.save(entity)).thenReturn(entity);
        when(paroisseMapper.modelToDto(entity)).thenReturn(expected);

        ParoisseResponse result = service.create(new ParoisseRequest(
                " Saint   Jean ", " 12 rue de la Paix ", " CONTACT@Example.COM ", " ", localiteId
        ));

        assertEquals(expected, result);
        assertEquals(localite, entity.getLocalite());
        assertTrue(entity.getIsActive());
        assertFalse(entity.getStatusDel());
    }

    @Test
    void rejectsDuplicateParishOnUpdate() {
        UUID publicId = UUID.randomUUID();
        UUID localiteId = UUID.randomUUID();
        Paroisse paroisse = paroisse(publicId, localite(localiteId));
        when(paroisseRepository.findByPublicIdAndStatusDelFalse(publicId)).thenReturn(Optional.of(paroisse));
        when(localiteRepository.findByPublicIdAndStatusDelFalse(localiteId))
                .thenReturn(Optional.of(paroisse.getLocalite()));
        when(paroisseRepository.existsByNomIgnoreCaseAndLocalite_PublicIdAndStatusDelFalseAndPublicIdNot(
                "Saint Jean", localiteId, publicId
        )).thenReturn(true);

        assertThrows(AlreadyExistException.class, () -> service.update(
                publicId,
                new ParoisseRequest("Saint Jean", "Adresse valide", null, null, localiteId)
        ));

        verify(paroisseMapper, never()).updateEntityFromDto(any(), any());
    }

    @Test
    void deletionAlsoRevokesParishAccesses() {
        UUID publicId = UUID.randomUUID();
        Paroisse paroisse = paroisse(publicId, localite(UUID.randomUUID()));
        ParoisseAccess access = new ParoisseAccess();
        access.setActive(true);
        access.setStatusDel(false);
        when(paroisseRepository.findByPublicIdAndStatusDelFalse(publicId)).thenReturn(Optional.of(paroisse));
        when(paroisseAccessRepository.findByParoisseAndStatusDelFalse(paroisse)).thenReturn(List.of(access));

        service.deleteByPublicId(publicId);

        assertFalse(paroisse.getIsActive());
        assertTrue(paroisse.getStatusDel());
        assertFalse(access.getActive());
        assertTrue(access.getStatusDel());
        verify(paroisseRepository).save(paroisse);
    }

    @Test
    void validatesLengthAfterNormalization() {
        assertThrows(IllegalArgumentException.class, () -> service.create(
                new ParoisseRequest(" A ", "Adresse valide", null, null, UUID.randomUUID())
        ));
        verify(tenantAccessService, never()).isGlobalUser();
    }

    private Localite localite(UUID publicId) {
        Localite localite = new Localite();
        localite.setPublicId(publicId);
        localite.setStatusDel(false);
        return localite;
    }

    private Paroisse paroisse(UUID publicId, Localite localite) {
        Paroisse paroisse = new Paroisse();
        paroisse.setPublicId(publicId);
        paroisse.setNom("Saint Jean");
        paroisse.setAdresse("Adresse valide");
        paroisse.setIsActive(true);
        paroisse.setStatusDel(false);
        paroisse.setLocalite(localite);
        return paroisse;
    }

    private ParoisseResponse response(UUID publicId, UUID localiteId) {
        return new ParoisseResponse(
                publicId, "Saint Jean", "12 rue de la Paix", "contact@example.com", null,
                true, localiteId, "Lomé", "Tokoin", null, null
        );
    }
}
