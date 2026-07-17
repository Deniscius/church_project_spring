package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.TypePaiementRequest;
import com.eyram.dev.church_project_spring.DTO.response.TypePaiementResponse;
import com.eyram.dev.church_project_spring.entities.TypePaiement;
import com.eyram.dev.church_project_spring.enums.ModePaiement;
import com.eyram.dev.church_project_spring.mappers.TypePaiementMapper;
import com.eyram.dev.church_project_spring.repositories.DemandeRepository;
import com.eyram.dev.church_project_spring.repositories.DetailsPaiementRepository;
import com.eyram.dev.church_project_spring.repositories.TypePaiementRepository;
import com.eyram.dev.church_project_spring.service.impl.TypePaiementServiceImpl;
import com.eyram.dev.church_project_spring.utils.exception.AlreadyExistException;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class TypePaiementServiceImplTest {

    @Mock
    private TypePaiementRepository typePaiementRepository;
    @Mock
    private TypePaiementMapper typePaiementMapper;
    @Mock
    private DemandeRepository demandeRepository;
    @Mock
    private DetailsPaiementRepository detailsPaiementRepository;

    @InjectMocks
    private TypePaiementServiceImpl service;

    @Test
    void createsNormalizedPaymentType() {
        TypePaiementRequest normalized = new TypePaiementRequest("Paiement TMoney", ModePaiement.TMONEY);
        TypePaiement entity = paymentType(UUID.randomUUID(), ModePaiement.TMONEY);
        TypePaiementResponse expected = new TypePaiementResponse(
                entity.getPublicId(), "Paiement TMoney", ModePaiement.TMONEY
        );

        when(typePaiementMapper.dtoToModel(normalized)).thenReturn(entity);
        when(typePaiementRepository.save(entity)).thenReturn(entity);
        when(typePaiementMapper.modelToDto(entity)).thenReturn(expected);

        TypePaiementResponse result = service.create(
                new TypePaiementRequest("  Paiement   TMoney ", ModePaiement.TMONEY)
        );

        assertEquals(expected, result);
        verify(typePaiementRepository).findByModeAndStatusDelFalse(ModePaiement.TMONEY);
        verify(typePaiementMapper).dtoToModel(normalized);
    }

    @Test
    void rejectsDuplicateModeOnCreation() {
        TypePaiement existing = paymentType(UUID.randomUUID(), ModePaiement.FLOOZ);
        when(typePaiementRepository.findByModeAndStatusDelFalse(ModePaiement.FLOOZ))
                .thenReturn(Optional.of(existing));

        assertThrows(
                AlreadyExistException.class,
                () -> service.create(new TypePaiementRequest("Flooz", ModePaiement.FLOOZ))
        );

        verify(typePaiementRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void refusesDeletionWhenPaymentTypeIsUsedByRequest() {
        UUID publicId = UUID.randomUUID();
        TypePaiement type = paymentType(publicId, ModePaiement.CARTE);
        when(typePaiementRepository.findByPublicIdAndStatusDelFalse(publicId))
                .thenReturn(Optional.of(type));
        when(demandeRepository.existsByTypePaiementAndStatusDelFalse(type)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> service.delete(publicId));

        assertFalse(type.getStatusDel());
        verify(typePaiementRepository, never()).save(type);
    }

    @Test
    void softDeletesUnusedPaymentType() {
        UUID publicId = UUID.randomUUID();
        TypePaiement type = paymentType(publicId, ModePaiement.ESPECES);
        when(typePaiementRepository.findByPublicIdAndStatusDelFalse(publicId))
                .thenReturn(Optional.of(type));

        service.delete(publicId);

        assertTrue(type.getStatusDel());
        verify(typePaiementRepository).save(type);
    }

    private TypePaiement paymentType(UUID publicId, ModePaiement mode) {
        TypePaiement type = new TypePaiement();
        type.setPublicId(publicId);
        type.setMode(mode);
        type.setStatusDel(false);
        return type;
    }
}
