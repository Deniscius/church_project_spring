package com.eyram.dev.church_project_spring.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.TypeDemande;
import com.eyram.dev.church_project_spring.mappers.ForfaitTarifMapper;
import com.eyram.dev.church_project_spring.repositories.ForfaitTarifRepository;
import com.eyram.dev.church_project_spring.repositories.TypeDemandeRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ForfaitTarifServiceImplTest {

    @Mock
    private ForfaitTarifRepository forfaitTarifRepository;
    @Mock
    private TypeDemandeRepository typeDemandeRepository;
    @Mock
    private ForfaitTarifMapper forfaitTarifMapper;
    @Mock
    private TenantAccessService tenantAccessService;

    @Test
    void getByTypeDemandeRejectsAnotherParishBeforeLoadingPricing() {
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

        ForfaitTarifServiceImpl service = new ForfaitTarifServiceImpl(
                forfaitTarifRepository,
                typeDemandeRepository,
                forfaitTarifMapper,
                tenantAccessService
        );

        assertThrows(AccessDeniedException.class, () -> service.getByTypeDemande(typeId));
        verify(forfaitTarifRepository, never())
                .findByTypeDemandeAndStatusDelFalse(typeDemande);
    }
}
