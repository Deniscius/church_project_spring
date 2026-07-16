package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.LocaliteRequest;
import com.eyram.dev.church_project_spring.DTO.response.LocaliteResponse;
import com.eyram.dev.church_project_spring.entities.Localite;
import com.eyram.dev.church_project_spring.mappers.LocaliteMapper;
import com.eyram.dev.church_project_spring.repositories.LocaliteRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.service.LocaliteService;
import com.eyram.dev.church_project_spring.utils.exception.AlreadyExistException;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class LocaliteServiceImpl implements LocaliteService {

    private final LocaliteRepository localiteRepository;
    private final ParoisseRepository paroisseRepository;
    private final LocaliteMapper localiteMapper;

    @Override
    public LocaliteResponse create(LocaliteRequest request) {
        LocaliteRequest normalizedRequest = normalize(request);

        boolean exists = localiteRepository
                .existsByVilleIgnoreCaseAndQuartierIgnoreCaseAndStatusDelFalse(
                        normalizedRequest.ville(),
                        normalizedRequest.quartier()
                );

        if (exists) {
            throw new AlreadyExistException("Cette localité existe déjà");
        }

        Localite localite = localiteMapper.dtoToModel(normalizedRequest);
        localite.setPublicId(UUID.randomUUID());
        localite.setStatusDel(false);

        Localite savedLocalite = localiteRepository.save(localite);
        return localiteMapper.modelToDto(savedLocalite);
    }

    @Override
    @Transactional(readOnly = true)
    public LocaliteResponse getByPublicId(UUID publicId) {
        Localite localite = findActiveLocalite(publicId);

        return localiteMapper.modelToDto(localite);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocaliteResponse> getAll() {
        return localiteRepository.findAllByStatusDelFalseOrderByVilleAscQuartierAsc()
                .stream()
                .map(localiteMapper::modelToDto)
                .toList();
    }

    @Override
    public LocaliteResponse update(UUID publicId, LocaliteRequest request) {
        LocaliteRequest normalizedRequest = normalize(request);
        Localite localite = findActiveLocalite(publicId);

        boolean duplicateExists = localiteRepository
                .existsByVilleIgnoreCaseAndQuartierIgnoreCaseAndStatusDelFalseAndPublicIdNot(
                        normalizedRequest.ville(),
                        normalizedRequest.quartier(),
                        publicId
                );
        if (duplicateExists) {
            throw new AlreadyExistException("Cette localité existe déjà");
        }

        localiteMapper.updateEntityFromDto(normalizedRequest, localite);

        Localite updatedLocalite = localiteRepository.save(localite);
        return localiteMapper.modelToDto(updatedLocalite);
    }

    @Override
    public void deleteByPublicId(UUID publicId) {
        Localite localite = findActiveLocalite(publicId);

        if (paroisseRepository.existsByLocaliteAndStatusDelFalse(localite)) {
            throw new BusinessRuleException(
                    "Cette localité ne peut pas être supprimée car elle est utilisée par une paroisse"
            );
        }

        localite.setStatusDel(true);
        localiteRepository.save(localite);
    }

    private Localite findActiveLocalite(UUID publicId) {
        if (publicId == null) {
            throw new IllegalArgumentException("L'identifiant de la localité est obligatoire");
        }

        return localiteRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Localité non trouvée"));
    }

    private LocaliteRequest normalize(LocaliteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La requête localité est obligatoire");
        }

        String ville = normalizeRequired(
                request.ville(),
                2,
                150,
                "La ville est obligatoire",
                "La ville doit contenir entre 2 et 150 caractères"
        );
        String quartier = normalizeRequired(
                request.quartier(),
                2,
                200,
                "Le quartier est obligatoire",
                "Le quartier doit contenir entre 2 et 200 caractères"
        );
        return new LocaliteRequest(ville, quartier);
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
