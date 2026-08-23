package com.eyram.dev.church_project_spring.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.TypeDemande;
import com.eyram.dev.church_project_spring.mappers.TypeDemandeMapper;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.repositories.TypeDemandeRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class TypeDemandeServiceImplTest {

    @Mock
    private TypeDemandeRepository typeDemandeRepository;
    @Mock
    private ParoisseRepository paroisseRepository;
    @Mock
    private TypeDemandeMapper typeDemandeMapper;
    @Mock
    private TenantAccessService tenantAccessService;

    @Test
    void getByPublicIdRejectsAnotherParishBeforeMappingResponse() {
        UUID typeId = UUID.randomUUID();
        Paroisse otherParish = new Paroisse();
        otherParish.setPublicId(UUID.randomUUID());

        TypeDemande typeDemande = new TypeDemande();
        typeDemande.setPublicId(typeId);
        typeDemande.setParoisse(otherParish);

        when(typeDemandeRepository.findByPublicIdAndStatusDelFalse(typeId))
                .thenReturn(Optional.of(typeDemande));
        org.mockito.Mockito.doThrow(new AccessDeniedException("Accès refusé"))
                .when(tenantAccessService).checkParoisseAccess(otherParish);

        TypeDemandeServiceImpl service = new TypeDemandeServiceImpl(
                typeDemandeRepository,
                paroisseRepository,
                typeDemandeMapper,
                tenantAccessService
        );

        assertThrows(AccessDeniedException.class, () -> service.getByPublicId(typeId));
        verify(typeDemandeMapper, never()).modelToDto(typeDemande);
    }
}
