package com.eyram.dev.church_project_spring.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.mappers.DemandeDateMapper;
import com.eyram.dev.church_project_spring.repositories.DemandeDateRepository;
import com.eyram.dev.church_project_spring.repositories.DemandeRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.service.DemandeSchedulingPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class DemandeDateServiceImplTest {

    @Mock
    private DemandeDateRepository demandeDateRepository;
    @Mock
    private DemandeRepository demandeRepository;
    @Mock
    private DemandeDateMapper demandeDateMapper;
    @Mock
    private TenantAccessService tenantAccessService;
    @Mock
    private DemandeSchedulingPolicy schedulingPolicy;

    @Test
    void getByDemandeRejectsAnotherParishBeforeLoadingDates() {
        UUID demandeId = UUID.randomUUID();
        Paroisse otherParish = new Paroisse();
        otherParish.setId(2L);
        otherParish.setPublicId(UUID.randomUUID());

        Demande demande = new Demande();
        demande.setPublicId(demandeId);
        demande.setParoisse(otherParish);

        when(demandeRepository.findByPublicIdAndStatusDelFalse(demandeId))
                .thenReturn(Optional.of(demande));
        org.mockito.Mockito.doThrow(new AccessDeniedException("Accès refusé"))
                .when(tenantAccessService).checkParoisseAccess(otherParish);

        DemandeDateServiceImpl service = new DemandeDateServiceImpl(
                demandeDateRepository,
                demandeRepository,
                demandeDateMapper,
                tenantAccessService,
                schedulingPolicy
        );

        assertThrows(AccessDeniedException.class, () -> service.getByDemande(demandeId));
        verify(demandeDateRepository, never())
                .findByDemandeAndStatusDelFalseOrderByOrdreAsc(demande);
    }
}
