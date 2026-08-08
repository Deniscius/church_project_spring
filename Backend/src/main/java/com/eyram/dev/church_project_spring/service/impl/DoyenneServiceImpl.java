package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.DoyenneRequest;
import com.eyram.dev.church_project_spring.DTO.response.DoyenneResponse;
import com.eyram.dev.church_project_spring.config.CacheConfig;
import com.eyram.dev.church_project_spring.entities.Doyenne;
import com.eyram.dev.church_project_spring.mappers.DoyenneMapper;
import com.eyram.dev.church_project_spring.repositories.DoyenneRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.service.DoyenneService;
import com.eyram.dev.church_project_spring.utils.exception.AlreadyExistException;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DoyenneServiceImpl implements DoyenneService {

    private final DoyenneRepository doyenneRepository;
    private final ParoisseRepository paroisseRepository;
    private final DoyenneMapper doyenneMapper;

    @Override
    @CacheEvict(cacheNames = CacheConfig.DOYENNES, allEntries = true)
    public DoyenneResponse create(DoyenneRequest request) {
        DoyenneRequest normalizedRequest = normalize(request);

        boolean exists = doyenneRepository
                .existsByNomIgnoreCaseAndStatusDelFalse(normalizedRequest.nom());

        if (exists) {
            throw new AlreadyExistException("Ce doyenné existe déjà");
        }

        Doyenne doyenne = doyenneMapper.dtoToModel(normalizedRequest);
        doyenne.setPublicId(UUID.randomUUID());
        doyenne.setStatusDel(false);
        doyenne.setRang(normalizedRequest.rang() != null
                ? normalizedRequest.rang()
                : doyenneRepository.findMaxRang() + 1);

        Doyenne savedDoyenne = doyenneRepository.save(doyenne);
        return doyenneMapper.modelToDto(savedDoyenne);
    }

    @Override
    @Transactional(readOnly = true)
    public DoyenneResponse getByPublicId(UUID publicId) {
        Doyenne doyenne = findActiveDoyenne(publicId);

        return doyenneMapper.modelToDto(doyenne);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.DOYENNES)
    public List<DoyenneResponse> getAll() {
        return doyenneRepository.findAllByStatusDelFalseOrderByRangAscNomAsc()
                .stream()
                .map(doyenneMapper::modelToDto)
                .toList();
    }

    @Override
    @CacheEvict(cacheNames = CacheConfig.DOYENNES, allEntries = true)
    public DoyenneResponse update(UUID publicId, DoyenneRequest request) {
        DoyenneRequest normalizedRequest = normalize(request);
        Doyenne doyenne = findActiveDoyenne(publicId);

        boolean duplicateExists = doyenneRepository
                .existsByNomIgnoreCaseAndStatusDelFalseAndPublicIdNot(
                        normalizedRequest.nom(),
                        publicId
                );
        if (duplicateExists) {
            throw new AlreadyExistException("Ce doyenné existe déjà");
        }

        doyenneMapper.updateEntityFromDto(normalizedRequest, doyenne);
        if (normalizedRequest.rang() != null) {
            doyenne.setRang(normalizedRequest.rang());
        }

        Doyenne updatedDoyenne = doyenneRepository.save(doyenne);
        return doyenneMapper.modelToDto(updatedDoyenne);
    }

    @Override
    @CacheEvict(cacheNames = CacheConfig.DOYENNES, allEntries = true)
    public void deleteByPublicId(UUID publicId) {
        Doyenne doyenne = findActiveDoyenne(publicId);

        if (paroisseRepository.existsByDoyenneAndStatusDelFalse(doyenne)) {
            throw new BusinessRuleException(
                    "Ce doyenné ne peut pas être supprimé car il contient une paroisse"
            );
        }

        doyenne.setStatusDel(true);
        doyenneRepository.save(doyenne);
    }

    private Doyenne findActiveDoyenne(UUID publicId) {
        if (publicId == null) {
            throw new IllegalArgumentException("L'identifiant du doyenné est obligatoire");
        }

        return doyenneRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Doyenné non trouvé"));
    }

    private DoyenneRequest normalize(DoyenneRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La requête doyenné est obligatoire");
        }

        String nom = normalizeRequired(
                request.nom(),
                2,
                400,
                "Le nom du doyenné est obligatoire",
                "Le nom doit contenir entre 2 et 400 caractères"
        );
        String description = request.description() == null || request.description().isBlank()
                ? null
                : request.description().strip().replaceAll("\\s+", " ");
        if (description != null && description.length() > 500) {
            throw new IllegalArgumentException("La description ne doit pas dépasser 500 caractères");
        }
        if (request.rang() != null && request.rang() < 1) {
            throw new IllegalArgumentException("Le rang doit être supérieur ou égal à 1");
        }
        return new DoyenneRequest(nom, description, request.rang());
    }

    private String normalizeRequired(
            String value,
            int minLength,
            int maxLength,
            String requiredMessage,
            String lengthMessage
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(requiredMessage);
        }

        String normalizedValue = value.strip().replaceAll("\\s+", " ");
        if (normalizedValue.length() < minLength || normalizedValue.length() > maxLength) {
            throw new IllegalArgumentException(lengthMessage);
        }
        return normalizedValue;
    }
}
