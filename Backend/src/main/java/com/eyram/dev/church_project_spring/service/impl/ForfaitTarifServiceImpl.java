package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.ForfaitTarifRequest;
import com.eyram.dev.church_project_spring.DTO.response.ForfaitTarifResponse;
import com.eyram.dev.church_project_spring.entities.ForfaitTarif;
import com.eyram.dev.church_project_spring.entities.TypeDemande;
import com.eyram.dev.church_project_spring.enums.JourSemaine;
import com.eyram.dev.church_project_spring.mappers.ForfaitTarifMapper;
import com.eyram.dev.church_project_spring.repositories.ForfaitTarifRepository;
import com.eyram.dev.church_project_spring.repositories.TypeDemandeRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.service.ForfaitTarifService;
import com.eyram.dev.church_project_spring.utils.exception.AlreadyExistException;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ForfaitTarifServiceImpl implements ForfaitTarifService {

    private final ForfaitTarifRepository forfaitTarifRepository;
    private final TypeDemandeRepository typeDemandeRepository;
    private final ForfaitTarifMapper forfaitTarifMapper;
    private final TenantAccessService tenantAccessService;

    @Override
    public ForfaitTarifResponse create(ForfaitTarifRequest request) {

        TypeDemande typeDemande = typeDemandeRepository.findByPublicIdAndStatusDelFalse(request.typeDemandePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Type de demande introuvable"));
        tenantAccessService.checkParoisseAccess(typeDemande.getParoisse());

        if (forfaitTarifRepository.existsByCodeForfaitAndStatusDelFalse(request.codeForfait())) {
            throw new AlreadyExistException("Un forfait avec ce code existe déjà");
        }

        if (forfaitTarifRepository.existsByNomForfaitIgnoreCaseAndTypeDemandeAndStatusDelFalse(
                request.nomForfait(), typeDemande)) {
            throw new AlreadyExistException("Un forfait avec ce nom existe déjà pour ce type de demande");
        }

        validateJoursCelebration(typeDemande, request.joursCelebrationAutorises());

        ForfaitTarif forfaitTarif = forfaitTarifMapper.dtoToModel(request);
        forfaitTarif.setCodeForfait(normalizeCode(request.codeForfait()));
        forfaitTarif.setNomForfait(normalizeText(request.nomForfait()));
        forfaitTarif.setLibelle(normalizeOptionalText(request.libelle()));
        forfaitTarif.setTypeDemande(typeDemande);
        applyJoursCelebration(forfaitTarif, request.joursCelebrationAutorises());

        ForfaitTarif savedForfaitTarif = forfaitTarifRepository.save(forfaitTarif);
        return forfaitTarifMapper.modelToDtoWithMeta(savedForfaitTarif);
    }

    @Override
    public ForfaitTarifResponse update(UUID publicId, ForfaitTarifRequest request) {

        ForfaitTarif existingForfaitTarif = forfaitTarifRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Forfait tarif introuvable"));
        tenantAccessService.checkParoisseAccess(existingForfaitTarif.getTypeDemande().getParoisse());

        TypeDemande typeDemande = typeDemandeRepository.findByPublicIdAndStatusDelFalse(request.typeDemandePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Type de demande introuvable"));
        tenantAccessService.checkParoisseAccess(typeDemande.getParoisse());

        if (!existingForfaitTarif.getCodeForfait().equals(request.codeForfait())
                && forfaitTarifRepository.existsByCodeForfaitAndStatusDelFalse(request.codeForfait())) {
            throw new AlreadyExistException("Un forfait avec ce code existe déjà");
        }

        boolean changed =
                !existingForfaitTarif.getNomForfait().equalsIgnoreCase(request.nomForfait()) ||
                        !existingForfaitTarif.getTypeDemande().getPublicId().equals(request.typeDemandePublicId());

        if (changed && forfaitTarifRepository.existsByNomForfaitIgnoreCaseAndTypeDemandeAndStatusDelFalse(
                request.nomForfait(), typeDemande)) {
            throw new AlreadyExistException("Un forfait avec ce nom existe déjà pour ce type de demande");
        }

        validateJoursCelebration(typeDemande, request.joursCelebrationAutorises());

        forfaitTarifMapper.updateEntityFromDto(request, existingForfaitTarif);
        existingForfaitTarif.setCodeForfait(normalizeCode(request.codeForfait()));
        existingForfaitTarif.setNomForfait(normalizeText(request.nomForfait()));
        existingForfaitTarif.setLibelle(normalizeOptionalText(request.libelle()));
        existingForfaitTarif.setTypeDemande(typeDemande);
        applyJoursCelebration(existingForfaitTarif, request.joursCelebrationAutorises());

        ForfaitTarif updatedForfaitTarif = forfaitTarifRepository.save(existingForfaitTarif);
        return forfaitTarifMapper.modelToDtoWithMeta(updatedForfaitTarif);
    }

    @Override
    public ForfaitTarifResponse getByPublicId(UUID publicId) {
        ForfaitTarif forfaitTarif = forfaitTarifRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Forfait tarif introuvable"));
        tenantAccessService.checkParoisseAccess(forfaitTarif.getTypeDemande().getParoisse());

        return forfaitTarifMapper.modelToDtoWithMeta(forfaitTarif);
    }

    @Override
    public List<ForfaitTarifResponse> getAll() {
        return forfaitTarifRepository.findByStatusDelFalse()
                .stream()
                .map(forfaitTarifMapper::modelToDtoWithMeta)
                .toList();
    }

    @Override
    public List<ForfaitTarifResponse> getByTypeDemande(UUID typeDemandePublicId) {
        TypeDemande typeDemande = typeDemandeRepository.findByPublicIdAndStatusDelFalse(typeDemandePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Type de demande introuvable"));

        return forfaitTarifRepository.findByTypeDemandeAndStatusDelFalse(typeDemande)
                .stream()
                .map(forfaitTarifMapper::modelToDtoWithMeta)
                .toList();
    }

    @Override
    public List<ForfaitTarifResponse> getActiveByTypeDemande(UUID typeDemandePublicId) {
        TypeDemande typeDemande = typeDemandeRepository.findByPublicIdAndStatusDelFalse(typeDemandePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Type de demande introuvable"));

        return forfaitTarifRepository.findByTypeDemandeAndIsActiveTrueAndStatusDelFalse(typeDemande)
                .stream()
                .map(forfaitTarifMapper::modelToDtoWithMeta)
                .toList();
    }

    @Override
    public void deleteByPublicId(UUID publicId) {
        ForfaitTarif forfaitTarif = forfaitTarifRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Forfait tarif introuvable"));
        tenantAccessService.checkParoisseAccess(forfaitTarif.getTypeDemande().getParoisse());

        forfaitTarif.setStatusDel(true);
        forfaitTarifRepository.save(forfaitTarif);
    }

    private void validateJoursCelebration(TypeDemande typeDemande, Set<JourSemaine> forfaitDays) {
        if (forfaitDays == null || forfaitDays.isEmpty()) {
            throw new BusinessRuleException("Sélectionnez au moins un jour de célébration compatible avec le type de demande");
        }

        Set<JourSemaine> typeDays = typeDemande.getJoursCelebrationAutorises();
        if (typeDays == null || typeDays.isEmpty()) {
            return;
        }

        if (!typeDays.containsAll(forfaitDays)) {
            throw new BusinessRuleException(
                    "Les jours du forfait doivent être compatibles avec ceux autorisés par le type de demande"
            );
        }
    }

    private void applyJoursCelebration(ForfaitTarif forfaitTarif, Set<JourSemaine> joursCelebrationAutorises) {
        forfaitTarif.getJoursCelebrationAutorises().clear();
        forfaitTarif.getJoursCelebrationAutorises().addAll(joursCelebrationAutorises);
    }

    private String normalizeCode(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toUpperCase();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
