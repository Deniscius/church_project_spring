package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseRequest;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseResponse;
import com.eyram.dev.church_project_spring.entities.Localite;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.mappers.ParoisseMapper;
import com.eyram.dev.church_project_spring.repositories.LocaliteRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseAccessRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.service.ParoisseService;
import com.eyram.dev.church_project_spring.utils.exception.AlreadyExistException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ParoisseServiceImpl implements ParoisseService {

    private final ParoisseRepository paroisseRepository;
    private final ParoisseAccessRepository paroisseAccessRepository;
    private final LocaliteRepository localiteRepository;
    private final ParoisseMapper paroisseMapper;
    private final TenantAccessService tenantAccessService;

    @Override
    public ParoisseResponse create(ParoisseRequest request) {
        ParoisseRequest normalizedRequest = normalize(request);

        if (!tenantAccessService.isGlobalUser()) {
            throw new AccessDeniedException("Seul un utilisateur global peut créer une paroisse");
        }

        Localite localite = findActiveLocalite(normalizedRequest.localitePublicId());

        boolean exists = paroisseRepository.existsByNomIgnoreCaseAndLocalite_PublicIdAndStatusDelFalse(
                normalizedRequest.nom(),
                normalizedRequest.localitePublicId()
        );

        if (exists) {
            throw new AlreadyExistException("Cette paroisse existe déjà dans cette localité");
        }

        Paroisse paroisse = paroisseMapper.dtoToModel(normalizedRequest);
        paroisse.setPublicId(UUID.randomUUID());
        paroisse.setStatusDel(false);
        paroisse.setIsActive(true);
        paroisse.setLocalite(localite);

        Paroisse savedParoisse = paroisseRepository.save(paroisse);
        return paroisseMapper.modelToDto(savedParoisse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParoisseResponse> getAll() {
        return paroisseRepository.findAllByStatusDelFalseAndIsActiveTrueOrderByNomAsc()
                .stream()
                .map(paroisseMapper::modelToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ParoisseResponse getByPublicId(UUID publicId) {
        Paroisse paroisse = findActiveParoisse(publicId);

        return paroisseMapper.modelToDto(paroisse);
    }

    @Override
    public ParoisseResponse update(UUID publicId, ParoisseRequest request) {
        ParoisseRequest normalizedRequest = normalize(request);
        Paroisse paroisse = findActiveParoisse(publicId);

        tenantAccessService.checkParoisseAccess(paroisse);

        Localite localite = findActiveLocalite(normalizedRequest.localitePublicId());

        boolean exists = paroisseRepository
                .existsByNomIgnoreCaseAndLocalite_PublicIdAndStatusDelFalseAndPublicIdNot(
                        normalizedRequest.nom(),
                        normalizedRequest.localitePublicId(),
                        publicId
        );

        if (exists) {
            throw new AlreadyExistException("Une autre paroisse avec ce nom existe déjà dans cette localité");
        }

        paroisseMapper.updateEntityFromDto(normalizedRequest, paroisse);
        paroisse.setLocalite(localite);

        Paroisse updatedParoisse = paroisseRepository.save(paroisse);
        return paroisseMapper.modelToDto(updatedParoisse);
    }

    @Override
    public void deleteByPublicId(UUID publicId) {
        Paroisse paroisse = findActiveParoisse(publicId);

        tenantAccessService.checkParoisseAccess(paroisse);

        paroisseAccessRepository.findByParoisseAndStatusDelFalse(paroisse)
                .forEach(access -> {
                    access.setActive(false);
                    access.setStatusDel(true);
                });
        paroisse.setIsActive(false);
        paroisse.setStatusDel(true);
        paroisseRepository.save(paroisse);
    }

    private Paroisse findActiveParoisse(UUID publicId) {
        if (publicId == null) {
            throw new IllegalArgumentException("L'identifiant de la paroisse est obligatoire");
        }
        return paroisseRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse non trouvée"));
    }

    private Localite findActiveLocalite(UUID publicId) {
        if (publicId == null) {
            throw new IllegalArgumentException("La localité est obligatoire");
        }
        return localiteRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Localité non trouvée"));
    }

    private ParoisseRequest normalize(ParoisseRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La requête paroisse est obligatoire");
        }

        return new ParoisseRequest(
                normalizeRequired(request.nom(), 2, 100,
                        "Le nom est obligatoire",
                        "Le nom doit contenir entre 2 et 100 caractères"),
                normalizeRequired(request.adresse(), 3, 200,
                        "L'adresse est obligatoire",
                        "L'adresse doit contenir entre 3 et 200 caractères"),
                normalizeEmail(request.email()),
                normalizeOptional(request.telephone(), 3, 50,
                        "Le téléphone doit contenir entre 3 et 50 caractères"),
                request.localitePublicId()
        );
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
        String normalized = value.strip().replaceAll("\\s+", " ");
        validateLength(normalized, minLength, maxLength, lengthMessage);
        return normalized;
    }

    private String normalizeOptional(String value, int minLength, int maxLength, String lengthMessage) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip().replaceAll("\\s+", " ");
        validateLength(normalized, minLength, maxLength, lengthMessage);
        return normalized;
    }

    private String normalizeEmail(String email) {
        String normalized = normalizeOptional(
                email,
                3,
                150,
                "L'email doit contenir entre 3 et 150 caractères"
        );
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private void validateLength(String value, int minLength, int maxLength, String message) {
        if (value.length() < minLength || value.length() > maxLength) {
            throw new IllegalArgumentException(message);
        }
    }
}
