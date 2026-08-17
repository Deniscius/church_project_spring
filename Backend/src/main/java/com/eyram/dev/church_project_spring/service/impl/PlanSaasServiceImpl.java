package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.PlanSaasRequest;
import com.eyram.dev.church_project_spring.DTO.response.PlanSaasResponse;
import com.eyram.dev.church_project_spring.entities.PlanSaas;
import com.eyram.dev.church_project_spring.enums.PlanAbonnement;
import com.eyram.dev.church_project_spring.mappers.PlanSaasMapper;
import com.eyram.dev.church_project_spring.repositories.PlanSaasRepository;
import com.eyram.dev.church_project_spring.service.PlanSaasService;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    public PlanSaasResponse update(UUID publicId, PlanSaasRequest request) {
        PlanSaas entity = repository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan SaaS introuvable"));

        mapper.updateEntityFromDto(request, entity);
        entity.setNom(request.nom().trim());
        entity.setDescription(clean(request.description()));

        if (!Boolean.TRUE.equals(entity.getActif())) {
            entity.setFeatured(false);
        } else if (Boolean.TRUE.equals(entity.getFeatured())) {
            repository.findByStatusDelFalseOrderByOrdreAffichageAscNomAsc()
                    .stream()
                    .filter(other -> !other.getId().equals(entity.getId()))
                    .filter(other -> Boolean.TRUE.equals(other.getFeatured()))
                    .forEach(other -> other.setFeatured(false));
        }

        return mapper.modelToDto(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public PlanSaas require(PlanAbonnement code) {
        if (code == null) {
            throw new BusinessRuleException("Le plan d'abonnement est obligatoire");
        }
        return repository.findByCodeAndStatusDelFalse(code)
                .orElseThrow(() -> new ResourceNotFoundException("Plan SaaS introuvable : " + code.name()));
    }

    @Override
    @Transactional(readOnly = true)
    public PlanSaas requireActive(PlanAbonnement code) {
        if (code == null) {
            throw new BusinessRuleException("Le plan d'abonnement est obligatoire");
        }
        return repository.findByCodeAndActifTrueAndStatusDelFalse(code)
                .orElseThrow(() -> new BusinessRuleException(
                        "Le plan " + code.name() + " n'est pas disponible actuellement"
                ));
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
