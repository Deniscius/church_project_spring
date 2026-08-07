package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.TypePaiementRequest;
import com.eyram.dev.church_project_spring.DTO.response.TypePaiementResponse;
import com.eyram.dev.church_project_spring.config.CacheConfig;
import com.eyram.dev.church_project_spring.entities.TypePaiement;
import com.eyram.dev.church_project_spring.mappers.TypePaiementMapper;
import com.eyram.dev.church_project_spring.repositories.TypePaiementRepository;
import com.eyram.dev.church_project_spring.repositories.DemandeRepository;
import com.eyram.dev.church_project_spring.repositories.DetailsPaiementRepository;
import com.eyram.dev.church_project_spring.service.TypePaiementService;
import com.eyram.dev.church_project_spring.utils.exception.AlreadyExistException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
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
public class TypePaiementServiceImpl implements TypePaiementService {

    private final TypePaiementRepository typePaiementRepository;
    private final TypePaiementMapper typePaiementMapper;
    private final DemandeRepository demandeRepository;
    private final DetailsPaiementRepository detailsPaiementRepository;

    @Override
    @CacheEvict(cacheNames = CacheConfig.TYPE_PAIEMENTS, allEntries = true)
    public TypePaiementResponse create(TypePaiementRequest request) {
        TypePaiementRequest normalizedRequest = normalize(request);
        typePaiementRepository.findByModeAndStatusDelFalse(normalizedRequest.mode())
                .ifPresent(existing -> {
                    throw new AlreadyExistException("Ce type de paiement existe déjà");
                });

        TypePaiement typePaiement = typePaiementMapper.dtoToModel(normalizedRequest);
        TypePaiement saved = typePaiementRepository.save(typePaiement);

        return typePaiementMapper.modelToDto(saved);
    }

    @Override
    public TypePaiementResponse getByPublicId(UUID publicId) {
        TypePaiement typePaiement = typePaiementRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Type de paiement introuvable"));

        return typePaiementMapper.modelToDto(typePaiement);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.TYPE_PAIEMENTS)
    public List<TypePaiementResponse> getAll() {
        return typePaiementRepository.findAllByStatusDelFalse()
                .stream()
                .map(typePaiementMapper::modelToDto)
                .toList();
    }

    @Override
    @CacheEvict(cacheNames = CacheConfig.TYPE_PAIEMENTS, allEntries = true)
    public TypePaiementResponse update(UUID publicId, TypePaiementRequest request) {
        TypePaiementRequest normalizedRequest = normalize(request);
        TypePaiement typePaiement = typePaiementRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Type de paiement introuvable"));

        typePaiementRepository.findByModeAndStatusDelFalse(normalizedRequest.mode())
                .ifPresent(existing -> {
                    if (!existing.getPublicId().equals(publicId)) {
                        throw new AlreadyExistException("Un type de paiement avec ce mode existe déjà");
                    }
                });

        typePaiementMapper.dtoToModel(normalizedRequest, typePaiement);
        TypePaiement updated = typePaiementRepository.save(typePaiement);

        return typePaiementMapper.modelToDto(updated);
    }

    @Override
    @CacheEvict(cacheNames = CacheConfig.TYPE_PAIEMENTS, allEntries = true)
    public void delete(UUID publicId) {
        TypePaiement typePaiement = typePaiementRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Type de paiement introuvable"));

        if (demandeRepository.existsByTypePaiementAndStatusDelFalse(typePaiement)
                || detailsPaiementRepository.existsByTypePaiementAndStatusDelFalse(typePaiement)) {
            throw new BusinessRuleException(
                    "Ce type de paiement ne peut pas être supprimé car il est déjà utilisé"
            );
        }

        typePaiement.setStatusDel(true);
        typePaiementRepository.save(typePaiement);
    }

    private TypePaiementRequest normalize(TypePaiementRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La requête type de paiement est obligatoire");
        }
        if (request.mode() == null) {
            throw new IllegalArgumentException("Le mode est obligatoire");
        }
        if (request.libelle() == null || request.libelle().isBlank()) {
            throw new IllegalArgumentException("Le libellé est obligatoire");
        }

        String normalizedLabel = request.libelle().strip().replaceAll("\\s+", " ");
        if (normalizedLabel.length() > 150) {
            throw new IllegalArgumentException("Le libellé ne doit pas dépasser 150 caractères");
        }
        return new TypePaiementRequest(normalizedLabel, request.mode());
    }
}
