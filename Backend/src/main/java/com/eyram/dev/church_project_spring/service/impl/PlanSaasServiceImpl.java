package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.PlanSaasCreateRequest;
import com.eyram.dev.church_project_spring.DTO.request.PlanSaasRequest;
import com.eyram.dev.church_project_spring.DTO.response.PlanSaasResponse;
import com.eyram.dev.church_project_spring.entities.PlanSaas;
import com.eyram.dev.church_project_spring.mappers.PlanSaasMapper;
import com.eyram.dev.church_project_spring.repositories.PlanSaasRepository;
import com.eyram.dev.church_project_spring.service.PlanSaasService;
import com.eyram.dev.church_project_spring.utils.exception.AlreadyExistException;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlanSaasServiceImpl implements PlanSaasService {

    private final PlanSaasRepository repository;
    private final PlanSaasMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<PlanSaasResponse> findAll() {
        return repository.findByStatusDelFalseOrderByOrdreAffichageAscNomAsc()
                .stream()
                .map(mapper::modelToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanSaasResponse> findPublicActive() {
        return repository.findByActifTrueAndStatusDelFalseOrderByOrdreAffichageAscNomAsc()
                .stream()
                .map(mapper::modelToDto)
                .toList();
    }

    @Override
    @Transactional
    public PlanSaasResponse create(PlanSaasCreateRequest request) {
        String code = normalizeCode(request.code());
        if (repository.existsByCodeIgnoreCaseAndStatusDelFalse(code)) {
            throw new AlreadyExistException("Un plan SaaS utilise déjà le code " + code);
        }

        PlanSaas entity = mapper.createEntityFromDto(request);
        entity.setCode(code);
        entity.setNom(request.nom().trim());
        entity.setDescription(clean(request.description()));
        entity.setActif(Boolean.TRUE.equals(request.actif()));
        entity.setFeatured(Boolean.TRUE.equals(request.actif()) && Boolean.TRUE.equals(request.featured()));
        entity.setStatusDel(false);

        if (Boolean.TRUE.equals(entity.getFeatured())) {
            clearFeaturedExcept(null);
        }

        return mapper.modelToDto(repository.save(entity));
    }

    @Override
    @Transactional
    public PlanSaasResponse update(UUID publicId, PlanSaasRequest request) {
        PlanSaas entity = repository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan SaaS introuvable"));

        mapper.updateEntityFromDto(request, entity);
        entity.setNom(request.nom().trim());
        entity.setDescription(clean(request.description()));

        if (!Boolean.TRUE.equals(entity.getActif())) {
            entity.setFeatured(false);
        } else if (Boolean.TRUE.equals(entity.getFeatured())) {
            clearFeaturedExcept(entity.getId());
        }

        return mapper.modelToDto(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public PlanSaas require(String code) {
        String normalized = normalizeCode(code);
        return repository.findByCodeAndStatusDelFalse(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Plan SaaS introuvable : " + normalized));
    }

    @Override
    @Transactional(readOnly = true)
    public PlanSaas requireActive(String code) {
        String normalized = normalizeCode(code);
        return repository.findByCodeAndActifTrueAndStatusDelFalse(normalized)
                .orElseThrow(() -> new BusinessRuleException(
                        "Le plan " + normalized + " n'est pas disponible actuellement"
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public PlanSaas requireDefaultActive() {
        List<PlanSaas> active = repository.findByActifTrueAndStatusDelFalseOrderByOrdreAffichageAscNomAsc();
        if (active.isEmpty()) {
            throw new BusinessRuleException("Aucune formule d'abonnement n'est disponible actuellement");
        }
        return active.stream()
                .filter(plan -> Boolean.TRUE.equals(plan.getFeatured()))
                .findFirst()
                .orElse(active.get(0));
    }

    private void clearFeaturedExcept(Long excludedId) {
        repository.findByStatusDelFalseOrderByOrdreAffichageAscNomAsc()
                .stream()
                .filter(other -> excludedId == null || !other.getId().equals(excludedId))
                .filter(other -> Boolean.TRUE.equals(other.getFeatured()))
                .forEach(other -> other.setFeatured(false));
    }

    private String normalizeCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new BusinessRuleException("Le code du plan d'abonnement est obligatoire");
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
