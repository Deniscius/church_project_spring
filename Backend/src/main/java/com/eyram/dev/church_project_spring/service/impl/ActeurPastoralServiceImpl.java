package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.ActeurPastoralRequest;
import com.eyram.dev.church_project_spring.DTO.response.ActeurPastoralResponse;
import com.eyram.dev.church_project_spring.entities.ActeurPastoral;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.mappers.ActeurPastoralMapper;
import com.eyram.dev.church_project_spring.repositories.ActeurPastoralRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.service.ActeurPastoralService;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ActeurPastoralServiceImpl implements ActeurPastoralService {

    private final ActeurPastoralRepository acteurPastoralRepository;
    private final ParoisseRepository paroisseRepository;
    private final ActeurPastoralMapper acteurPastoralMapper;
    private final TenantAccessService tenantAccessService;

    @Override
    public ActeurPastoralResponse create(ActeurPastoralRequest request) {
        Paroisse paroisse = requireParoisse(request.paroissePublicId());
        tenantAccessService.checkParoisseAccess(paroisse);

        ActeurPastoral entity = acteurPastoralMapper.dtoToModel(request);
        entity.setParoisse(paroisse);
        applyNormalizedValues(entity, request);
        entity.setActif(true);
        entity.setStatusDel(false);

        return acteurPastoralMapper.modelToDto(acteurPastoralRepository.saveAndFlush(entity));
    }

    @Override
    public ActeurPastoralResponse update(UUID publicId, ActeurPastoralRequest request) {
        ActeurPastoral entity = requireActeur(publicId);
        tenantAccessService.checkParoisseAccess(entity.getParoisse());
        assertVersion(entity, request.version());

        if (!entity.getParoisse().getPublicId().equals(request.paroissePublicId())) {
            throw new BusinessRuleException("Un acteur pastoral ne peut pas être déplacé vers une autre paroisse");
        }

        acteurPastoralMapper.updateEntityFromDto(request, entity);
        applyNormalizedValues(entity, request);
        return acteurPastoralMapper.modelToDto(acteurPastoralRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public ActeurPastoralResponse getByPublicId(UUID publicId) {
        ActeurPastoral entity = requireActeur(publicId);
        tenantAccessService.checkParoisseAccess(entity.getParoisse());
        return acteurPastoralMapper.modelToDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActeurPastoralResponse> getByParoisse(
            UUID paroissePublicId,
            Boolean actif,
            String recherche
    ) {
        Paroisse paroisse = requireParoisse(paroissePublicId);
        tenantAccessService.checkParoisseAccess(paroisse);
        String searchPattern = normalizeSearch(recherche);

        return acteurPastoralRepository.searchByParoisse(paroisse, actif, searchPattern)
                .stream()
                .map(acteurPastoralMapper::modelToDto)
                .toList();
    }

    @Override
    public ActeurPastoralResponse setActif(UUID publicId, Boolean actif, Long version) {
        ActeurPastoral entity = requireActeur(publicId);
        tenantAccessService.checkParoisseAccess(entity.getParoisse());
        assertVersion(entity, version);

        if (actif == null) {
            throw new BusinessRuleException("Le statut est obligatoire");
        }

        entity.setActif(actif);
        return acteurPastoralMapper.modelToDto(acteurPastoralRepository.saveAndFlush(entity));
    }

    @Override
    public void archive(UUID publicId, Long version) {
        ActeurPastoral entity = requireActeur(publicId);
        tenantAccessService.checkParoisseAccess(entity.getParoisse());
        assertVersion(entity, version);

        if (Boolean.TRUE.equals(entity.getActif())) {
            throw new BusinessRuleException("Désactivez l'acteur pastoral avant de l'archiver");
        }

        entity.setStatusDel(true);
        acteurPastoralRepository.save(entity);
    }

    private ActeurPastoral requireActeur(UUID publicId) {
        return acteurPastoralRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Acteur pastoral introuvable"));
    }

    private Paroisse requireParoisse(UUID publicId) {
        return paroisseRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));
    }

    private void applyNormalizedValues(ActeurPastoral entity, ActeurPastoralRequest request) {
        entity.setNom(normalizeRequired(request.nom(), "Le nom est obligatoire"));
        entity.setPrenoms(normalizeRequired(request.prenoms(), "Les prénoms sont obligatoires"));
        entity.setAppellation(normalizeNullable(request.appellation()));
        entity.setTelephone(normalizeNullable(request.telephone()));
        entity.setEmail(normalizeEmail(request.email()));
        entity.setNotes(normalizeNullable(request.notes()));
    }

    private void assertVersion(ActeurPastoral entity, Long requestedVersion) {
        if (requestedVersion == null) {
            throw new BusinessRuleException("La version est obligatoire pour cette opération");
        }
        if (!Objects.equals(entity.getVersion(), requestedVersion)) {
            throw new BusinessRuleException(
                    "Cet acteur pastoral a été modifié par un autre utilisateur ; rechargez les données"
            );
        }
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException(message);
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeEmail(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeSearch(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null ? null : "%" + normalized.toLowerCase(Locale.ROOT) + "%";
    }
}
