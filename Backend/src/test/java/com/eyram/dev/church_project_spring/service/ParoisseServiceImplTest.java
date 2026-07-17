package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseRequest;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseResponse;
import com.eyram.dev.church_project_spring.entities.Doyenne;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.ParoisseAccess;
import com.eyram.dev.church_project_spring.mappers.ParoisseMapper;
import com.eyram.dev.church_project_spring.repositories.DoyenneRepository;
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
    @Mock private DoyenneRepository doyenneRepository;
    @Mock private ParoisseMapper paroisseMapper;
    @Mock private TenantAccessService tenantAccessService;
    @InjectMocks private ParoisseServiceImpl service;

    @Test
    void createsNormalizedParishInAnActiveDeanery() {
        UUID doyenneId = UUID.randomUUID();
        Doyenne doyenne = doyenne(doyenneId);
        Paroisse entity = new Paroisse();
        ParoisseRequest normalized = new ParoisseRequest(
                "Saint Jean", "12 rue de la Paix", "contact@example.com", null, doyenneId
        );
        ParoisseResponse expected = response(UUID.randomUUID(), doyenneId);
        when(tenantAccessService.isGlobalUser()).thenReturn(true);
        when(doyenneRepository.findByPublicIdAndStatusDelFalse(doyenneId)).thenReturn(Optional.of(doyenne));
        when(paroisseMapper.dtoToModel(normalized)).thenReturn(entity);
        when(paroisseRepository.save(entity)).thenReturn(entity);
        when(paroisseMapper.modelToDto(entity)).thenReturn(expected);

        ParoisseResponse result = service.create(new ParoisseRequest(
                " Saint   Jean ", " 12 rue de la Paix ", " CONTACT@Example.COM ", " ", doyenneId
        ));

        assertEquals(expected, result);
        assertEquals(doyenne, entity.getDoyenne());
        assertTrue(entity.getIsActive());
        assertFalse(entity.getStatusDel());
    }

    @Test
    void rejectsDuplicateParishOnUpdate() {
        UUID publicId = UUID.randomUUID();
        UUID doyenneId = UUID.randomUUID();
        Paroisse paroisse = paroisse(publicId, doyenne(doyenneId));
        when(paroisseRepository.findByPublicIdAndStatusDelFalse(publicId)).thenReturn(Optional.of(paroisse));
        when(doyenneRepository.findByPublicIdAndStatusDelFalse(doyenneId))
                .thenReturn(Optional.of(paroisse.getDoyenne()));
        when(paroisseRepository.existsByNomIgnoreCaseAndDoyenne_PublicIdAndStatusDelFalseAndPublicIdNot(
                "Saint Jean", doyenneId, publicId
        )).thenReturn(true);

        assertThrows(AlreadyExistException.class, () -> service.update(
                publicId,
                new ParoisseRequest("Saint Jean", "Adresse valide", null, null, doyenneId)
        ));

        verify(paroisseMapper, never()).updateEntityFromDto(any(), any());
    }

    @Test
    void deletionAlsoRevokesParishAccesses() {
        UUID publicId = UUID.randomUUID();
        Paroisse paroisse = paroisse(publicId, doyenne(UUID.randomUUID()));
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

    private Doyenne doyenne(UUID publicId) {
        Doyenne doyenne = new Doyenne();
        doyenne.setPublicId(publicId);
        doyenne.setStatusDel(false);
        return doyenne;
    }

    private Paroisse paroisse(UUID publicId, Doyenne doyenne) {
        Paroisse paroisse = new Paroisse();
        paroisse.setPublicId(publicId);
        paroisse.setNom("Saint Jean");
        paroisse.setAdresse("Adresse valide");
        paroisse.setIsActive(true);
        paroisse.setStatusDel(false);
        paroisse.setDoyenne(doyenne);
        return paroisse;
    }

    private ParoisseResponse response(UUID publicId, UUID doyenneId) {
        return new ParoisseResponse(
                publicId, "Saint Jean", "12 rue de la Paix", "contact@example.com", null,
                true, doyenneId, "Doyenné de Lomé-Centre", null, null
        );
    }
}
