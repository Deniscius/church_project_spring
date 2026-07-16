package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.DemandeRequest;
import com.eyram.dev.church_project_spring.DTO.request.DemandeValidationRequest;
import com.eyram.dev.church_project_spring.DTO.response.DemandeResponse;
import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.TypeDemande;
import com.eyram.dev.church_project_spring.entities.User;
import com.eyram.dev.church_project_spring.enums.StatutDemandeEnum;
import com.eyram.dev.church_project_spring.enums.StatutValidationEnum;
import com.eyram.dev.church_project_spring.mappers.DemandeMapper;
import com.eyram.dev.church_project_spring.repositories.DemandeDateRepository;
import com.eyram.dev.church_project_spring.repositories.DemandeRepository;
import com.eyram.dev.church_project_spring.repositories.DetailsPaiementRepository;
import com.eyram.dev.church_project_spring.repositories.FactureRepository;
import com.eyram.dev.church_project_spring.repositories.ForfaitTarifRepository;
import com.eyram.dev.church_project_spring.repositories.HoraireRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.repositories.TypeDemandeRepository;
import com.eyram.dev.church_project_spring.repositories.TypePaiementRepository;
import com.eyram.dev.church_project_spring.repositories.UserRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.service.impl.DemandeServiceImpl;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Demande tenant consistency tests")
class DemandeServiceTenantConsistencyTest {

    @Mock
    private DemandeRepository demandeRepository;
    @Mock
    private ParoisseRepository paroisseRepository;
    @Mock
    private TypeDemandeRepository typeDemandeRepository;
    @Mock
    private ForfaitTarifRepository forfaitTarifRepository;
    @Mock
    private HoraireRepository horaireRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DemandeMapper demandeMapper;
    @Mock
    private TypePaiementRepository typePaiementRepository;
    @Mock
    private FactureRepository factureRepository;
    @Mock
    private DetailsPaiementRepository detailsPaiementRepository;
    @Mock
    private DemandeDateRepository demandeDateRepository;
    @Mock
    private TenantAccessService tenantAccessService;

    @InjectMocks
    private DemandeServiceImpl demandeService;

    @Test
    @DisplayName("Creation rejects a request type owned by another parish")
    void createRejectsTypeDemandeFromAnotherParish() {
        UUID selectedParishId = UUID.randomUUID();
        UUID typeDemandeId = UUID.randomUUID();
        UUID forfaitId = UUID.randomUUID();

        Paroisse selectedParish = parish(selectedParishId);
        TypeDemande foreignType = typeDemande(typeDemandeId, parish(UUID.randomUUID()));

        when(paroisseRepository.findByPublicIdAndStatusDelFalse(selectedParishId))
                .thenReturn(Optional.of(selectedParish));
        when(typeDemandeRepository.findByPublicIdAndStatusDelFalse(typeDemandeId))
                .thenReturn(Optional.of(foreignType));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> demandeService.create(request(selectedParishId, typeDemandeId, forfaitId))
        );

        assertEquals(
                "Le type de demande ne correspond pas à la paroisse choisie",
                exception.getMessage()
        );
        verify(forfaitTarifRepository, never()).findByPublicIdAndStatusDelFalse(any());
    }

    @Test
    @DisplayName("Update rejects a request type owned by another parish")
    void updateRejectsTypeDemandeFromAnotherParish() {
        UUID demandeId = UUID.randomUUID();
        UUID selectedParishId = UUID.randomUUID();
        UUID typeDemandeId = UUID.randomUUID();
        UUID forfaitId = UUID.randomUUID();

        Paroisse existingParish = parish(UUID.randomUUID());
        Paroisse selectedParish = parish(selectedParishId);

        Demande existingDemande = new Demande();
        existingDemande.setParoisse(existingParish);

        TypeDemande foreignType = typeDemande(typeDemandeId, parish(UUID.randomUUID()));

        when(demandeRepository.findByPublicIdAndStatusDelFalse(demandeId))
                .thenReturn(Optional.of(existingDemande));
        when(paroisseRepository.findByPublicIdAndStatusDelFalse(selectedParishId))
                .thenReturn(Optional.of(selectedParish));
        when(typeDemandeRepository.findByPublicIdAndStatusDelFalse(typeDemandeId))
                .thenReturn(Optional.of(foreignType));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> demandeService.update(
                        demandeId,
                        request(selectedParishId, typeDemandeId, forfaitId)
                )
        );

        assertEquals(
                "Le type de demande ne correspond pas à la paroisse choisie",
                exception.getMessage()
        );
        verify(tenantAccessService).checkParoisseAccess(existingParish);
        verify(tenantAccessService).checkParoisseAccess(selectedParish);
        verify(forfaitTarifRepository, never()).findByPublicIdAndStatusDelFalse(any());
    }

    @Test
    @DisplayName("Validation accepts a final status and records the validator")
    void validationRecordsValidator() {
        UUID demandeId = UUID.randomUUID();
        Paroisse paroisse = parish(UUID.randomUUID());
        Demande demande = new Demande();
        demande.setPublicId(demandeId);
        demande.setParoisse(paroisse);

        User validator = new User();
        validator.setNom("KOFFI");
        validator.setPrenom("Paul");

        DemandeResponse mappedResponse = response(
                demandeId,
                StatutValidationEnum.VALIDEE,
                StatutDemandeEnum.VALIDEE,
                validator.getFullName()
        );

        when(demandeRepository.findByPublicIdAndStatusDelFalse(demandeId))
                .thenReturn(Optional.of(demande));
        when(tenantAccessService.getCurrentUser()).thenReturn(validator);
        when(demandeRepository.save(demande)).thenReturn(demande);
        when(demandeMapper.modelToDto(demande)).thenReturn(mappedResponse);
        when(factureRepository.findByDemandePublicIdAndStatusDelFalse(demandeId))
                .thenReturn(Optional.empty());

        DemandeResponse result = demandeService.updateValidation(
                demandeId,
                new DemandeValidationRequest(StatutValidationEnum.VALIDEE)
        );

        assertEquals(StatutValidationEnum.VALIDEE, demande.getStatutValidation());
        assertEquals(StatutDemandeEnum.VALIDEE, demande.getStatutDemande());
        assertEquals(validator.getFullName(), demande.getValidateBy());
        assertEquals(StatutValidationEnum.VALIDEE, result.statutValidation());
        verify(tenantAccessService).checkParoisseAccess(paroisse);
    }

    @Test
    @DisplayName("Validation rejects the non-final pending status")
    void validationRejectsPendingStatus() {
        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> demandeService.updateValidation(
                        UUID.randomUUID(),
                        new DemandeValidationRequest(StatutValidationEnum.EN_ATTENTE)
                )
        );

        assertEquals("Une validation doit être acceptée ou rejetée", exception.getMessage());
        verify(demandeRepository, never()).findByPublicIdAndStatusDelFalse(any());
    }

    private Paroisse parish(UUID publicId) {
        Paroisse paroisse = new Paroisse();
        paroisse.setPublicId(publicId);
        return paroisse;
    }

    private TypeDemande typeDemande(UUID publicId, Paroisse paroisse) {
        TypeDemande typeDemande = new TypeDemande();
        typeDemande.setPublicId(publicId);
        typeDemande.setParoisse(paroisse);
        return typeDemande;
    }

    private DemandeRequest request(
            UUID paroissePublicId,
            UUID typeDemandePublicId,
            UUID forfaitTarifPublicId
    ) {
        return new DemandeRequest(
                "Intention de test",
                "KOFFI",
                "Eyram",
                "+22890000000",
                "eyram@example.com",
                null,
                null,
                LocalDate.now().plusDays(1),
                paroissePublicId,
                typeDemandePublicId,
                forfaitTarifPublicId,
                null,
                null,
                UUID.randomUUID()
        );
    }

    private DemandeResponse response(
            UUID publicId,
            StatutValidationEnum validation,
            StatutDemandeEnum statut,
            String validateBy
    ) {
        return new DemandeResponse(
                publicId, null, null, null, null, null, null, null, null,
                null, validation, validateBy, null, statut,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, false, null, null, null, null, null, null, null
        );
    }
}
