package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.StructurePastoraleRequest;
import com.eyram.dev.church_project_spring.DTO.response.StructurePastoraleResponse;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.StructurePastorale;
import com.eyram.dev.church_project_spring.mappers.StructurePastoraleMapper;
import com.eyram.dev.church_project_spring.repositories.MandatPastoralRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.repositories.StructurePastoraleRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.service.StructurePastoraleService;
import com.eyram.dev.church_project_spring.utils.exception.AlreadyExistException;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class StructurePastoraleServiceImpl implements StructurePastoraleService {

    private final StructurePastoraleRepository structureRepository;
    private final MandatPastoralRepository mandatRepository;
    private final ParoisseRepository paroisseRepository;
    private final StructurePastoraleMapper mapper;
    private final TenantAccessService tenantAccessService;

    @Override
    public StructurePastoraleResponse create(StructurePastoraleRequest request) {
        Paroisse paroisse = requireParoisse(request.paroissePublicId());
        tenantAccessService.checkParoisseAccess(paroisse);
        String nom = normalizeRequired(request.nom());

        if (structureRepository.existsByParoisseAndTypeAndNomIgnoreCaseAndStatusDelFalse(
                paroisse, request.type(), nom)) {
            throw new AlreadyExistException("Cette structure pastorale existe déjà");
        }

        StructurePastorale entity = mapper.dtoToModel(request);
        entity.setParoisse(paroisse);
        entity.setNom(nom);
        entity.setAttributions(normalizeNullable(request.attributions()));
        entity.setOrdreAffichage(defaultOrder(request.ordreAffichage()));
        entity.setActif(true);
        entity.setStatusDel(false);
        return mapper.modelToDto(structureRepository.saveAndFlush(entity));
    }

    @Override
    public StructurePastoraleResponse update(UUID publicId, StructurePastoraleRequest request) {
        StructurePastorale entity = requireStructure(publicId);
        tenantAccessService.checkParoisseAccess(entity.getParoisse());
        assertVersion(entity, request.version());

        if (!entity.getParoisse().getPublicId().equals(request.paroissePublicId())) {
            throw new BusinessRuleException("Une structure pastorale ne peut pas changer de paroisse");
        }

        String nom = normalizeRequired(request.nom());
        if (structureRepository
                .existsByParoisseAndTypeAndNomIgnoreCaseAndStatusDelFalseAndPublicIdNot(
                        entity.getParoisse(), request.type(), nom, publicId)) {
            throw new AlreadyExistException("Cette structure pastorale existe déjà");
        }

        mapper.updateEntityFromDto(request, entity);
        entity.setNom(nom);
        entity.setAttributions(normalizeNullable(request.attributions()));
        entity.setOrdreAffichage(defaultOrder(request.ordreAffichage()));
        return mapper.modelToDto(structureRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public StructurePastoraleResponse getByPublicId(UUID publicId) {
        StructurePastorale entity = requireStructure(publicId);
        tenantAccessService.checkParoisseAccess(entity.getParoisse());
        return mapper.modelToDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StructurePastoraleResponse> getByParoisse(UUID paroissePublicId) {
        Paroisse paroisse = requireParoisse(paroissePublicId);
        tenantAccessService.checkParoisseAccess(paroisse);
        return structureRepository
                .findByParoisseAndStatusDelFalseOrderByTypeAscOrdreAffichageAscNomAsc(paroisse)
                .stream()
                .map(mapper::modelToDto)
                .toList();
    }

    @Override
    public StructurePastoraleResponse setActif(UUID publicId, Boolean actif, Long version) {
        StructurePastorale entity = requireStructure(publicId);
        tenantAccessService.checkParoisseAccess(entity.getParoisse());
        assertVersion(entity, version);
        if (actif == null) {
            throw new BusinessRuleException("Le statut est obligatoire");
        }
        entity.setActif(actif);
        return mapper.modelToDto(structureRepository.saveAndFlush(entity));
    }

    @Override
    public void archive(UUID publicId, Long version) {
        StructurePastorale entity = requireStructure(publicId);
        tenantAccessService.checkParoisseAccess(entity.getParoisse());
        assertVersion(entity, version);

        if (Boolean.TRUE.equals(entity.getActif())) {
            throw new BusinessRuleException("Désactivez la structure avant de l'archiver");
        }
        if (mandatRepository.existsByStructurePastoraleAndStatusDelFalse(entity)) {
            throw new BusinessRuleException(
                    "Cette structure possède un historique de mandats et doit être conservée"
            );
        }

        entity.setStatusDel(true);
        structureRepository.save(entity);
    }

    private StructurePastorale requireStructure(UUID publicId) {
        return structureRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Structure pastorale introuvable"));
    }

    private Paroisse requireParoisse(UUID publicId) {
        return paroisseRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));
    }

    private void assertVersion(StructurePastorale entity, Long requestedVersion) {
        if (requestedVersion == null) {
            throw new BusinessRuleException("La version est obligatoire pour cette opération");
        }
        if (!Objects.equals(entity.getVersion(), requestedVersion)) {
            throw new BusinessRuleException(
                    "Cette structure a été modifiée par un autre utilisateur ; rechargez les données"
            );
        }
    }

    private String normalizeRequired(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException("Le nom de la structure est obligatoire");
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private int defaultOrder(Integer value) {
        return value == null ? 0 : value;
    }
}
