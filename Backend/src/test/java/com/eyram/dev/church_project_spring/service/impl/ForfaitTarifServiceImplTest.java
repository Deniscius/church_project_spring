package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.ForfaitTarifRequest;
import com.eyram.dev.church_project_spring.entities.ForfaitTarif;
import com.eyram.dev.church_project_spring.entities.TypeDemande;
import com.eyram.dev.church_project_spring.enums.JourSemaine;
import com.eyram.dev.church_project_spring.mappers.ForfaitTarifMapper;
import com.eyram.dev.church_project_spring.repositories.ForfaitTarifRepository;
import com.eyram.dev.church_project_spring.repositories.TypeDemandeRepository;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForfaitTarifServiceImplTest {

    @Mock
    private ForfaitTarifRepository forfaitTarifRepository;

    @Mock
    private TypeDemandeRepository typeDemandeRepository;

    @Mock
    private ForfaitTarifMapper forfaitTarifMapper;

    @InjectMocks
    private ForfaitTarifServiceImpl forfaitTarifService;

    @Test
    void create_shouldTrimAndNormalizeForfaitValuesBeforePersisting() {
        UUID typeDemandePublicId = UUID.randomUUID();
        TypeDemande typeDemande = new TypeDemande();
        typeDemande.setJoursCelebrationAutorises(Set.of(JourSemaine.LUNDI, JourSemaine.MARDI));

        ForfaitTarifRequest request = new ForfaitTarifRequest(
                "  FOR-001  ",
                "  Forfait mariage  ",
                new BigDecimal("150.00"),
                1,
                2,
                Set.of(JourSemaine.LUNDI),
                false,
                "  Service de mariage  ",
                true,
                typeDemandePublicId
        );

        ForfaitTarif mappedEntity = new ForfaitTarif();

        when(typeDemandeRepository.findByPublicIdAndStatusDelFalse(typeDemandePublicId)).thenReturn(Optional.of(typeDemande));
        when(forfaitTarifRepository.existsByCodeForfaitAndStatusDelFalse(anyString())).thenReturn(false);
        when(forfaitTarifRepository.existsByNomForfaitIgnoreCaseAndTypeDemandeAndStatusDelFalse(anyString(), any(TypeDemande.class))).thenReturn(false);
        when(forfaitTarifMapper.dtoToModel(request)).thenReturn(mappedEntity);
        when(forfaitTarifRepository.save(any(ForfaitTarif.class))).thenAnswer(invocation -> invocation.getArgument(0));

        forfaitTarifService.create(request);

        ArgumentCaptor<ForfaitTarif> captor = ArgumentCaptor.forClass(ForfaitTarif.class);
        verify(forfaitTarifRepository).save(captor.capture());

        ForfaitTarif persisted = captor.getValue();
        assertThat(persisted.getCodeForfait()).isEqualTo("FOR-001");
        assertThat(persisted.getNomForfait()).isEqualTo("Forfait mariage");
        assertThat(persisted.getLibelle()).isEqualTo("Service de mariage");
    }

    @Test
    void create_shouldExplainWhenForfaitDaysAreNotCompatibleWithType() {
        UUID typeDemandePublicId = UUID.randomUUID();
        TypeDemande typeDemande = new TypeDemande();
        typeDemande.setJoursCelebrationAutorises(Set.of(JourSemaine.LUNDI, JourSemaine.MARDI));

        ForfaitTarifRequest request = new ForfaitTarifRequest(
                "FOR-002",
                "Forfait incompatible",
                new BigDecimal("200.00"),
                1,
                2,
                Set.of(JourSemaine.MERCREDI),
                false,
                "Service",
                true,
                typeDemandePublicId
        );

        when(typeDemandeRepository.findByPublicIdAndStatusDelFalse(typeDemandePublicId)).thenReturn(Optional.of(typeDemande));
        when(forfaitTarifRepository.existsByCodeForfaitAndStatusDelFalse(anyString())).thenReturn(false);
        when(forfaitTarifRepository.existsByNomForfaitIgnoreCaseAndTypeDemandeAndStatusDelFalse(anyString(), any(TypeDemande.class))).thenReturn(false);
        when(forfaitTarifMapper.dtoToModel(request)).thenReturn(new ForfaitTarif());

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> forfaitTarifService.create(request));

        assertThat(exception.getMessage())
                .contains("compatibles")
                .contains("type de demande");
    }
}
