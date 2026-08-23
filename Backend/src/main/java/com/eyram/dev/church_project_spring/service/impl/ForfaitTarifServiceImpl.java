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
import com.eyram.dev.church_project_spring.utils.BusinessCodeGenerator;
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

        String codeForfait = resolveCreateCode(request, typeDemande);
        if (forfaitTarifRepository.existsByCodeForfaitAndStatusDelFalse(codeForfait)) {
            throw new AlreadyExistException("Un forfait avec ce code existe déjà");
        }

        if (forfaitTarifRepository.existsByNomForfaitIgnoreCaseAndTypeDemandeAndStatusDelFalse(
                request.nomForfait(), typeDemande)) {
            throw new AlreadyExistException("Un forfait avec ce nom existe déjà pour ce type de demande");
        }

        validateJoursCelebration(typeDemande, request.joursCelebrationAutorises());

        ForfaitTarif forfaitTarif = forfaitTarifMapper.dtoToModel(request);
        forfaitTarif.setCodeForfait(codeForfait);
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

        String nextCode = resolveUpdateCode(existingForfaitTarif, request.codeForfait());
        if (!existingForfaitTarif.getCodeForfait().equals(nextCode)
                && forfaitTarifRepository.existsByCodeForfaitAndStatusDelFalse(nextCode)) {
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
        existingForfaitTarif.setCodeForfait(nextCode);
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
        tenantAccessService.checkParoisseAccess(typeDemande.getParoisse());

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

    private String resolveCreateCode(ForfaitTarifRequest request, TypeDemande typeDemande) {
        String provided = normalizeCode(request.codeForfait());
        if (provided != null) {
            return provided;
        }
        String parishName = typeDemande.getParoisse() != null ? typeDemande.getParoisse().getNom() : null;
        return BusinessCodeGenerator.unique(
                BusinessCodeGenerator.forfaitCode(
                        parishName,
                        typeDemande.getTypeDemandeEnum(),
                        request.natureForfait(),
                        request.nombreCelebration(),
                        request.nombreJour()
                ),
                forfaitTarifRepository::existsByCodeForfaitAndStatusDelFalse
        );
    }

    /** Conservé si le client n’envoie pas de code (formulaire admin en lecture seule). */
    private String resolveUpdateCode(ForfaitTarif existing, String requestedCode) {
        String provided = normalizeCode(requestedCode);
        return provided != null ? provided : existing.getCodeForfait();
    }

    private String normalizeCode(String value) {
        if (value == null || value.isBlank()) {
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
