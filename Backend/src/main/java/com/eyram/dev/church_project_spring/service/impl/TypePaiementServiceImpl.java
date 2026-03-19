package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.TypePaiementRequest;
import com.eyram.dev.church_project_spring.DTO.response.TypePaiementResponse;
import com.eyram.dev.church_project_spring.entities.TypePaiement;
import com.eyram.dev.church_project_spring.mappers.TypePaiementMapper;
import com.eyram.dev.church_project_spring.repositories.TypePaiementRepository;
import com.eyram.dev.church_project_spring.service.TypePaiementService;
import com.eyram.dev.church_project_spring.utils.exception.AlreadyExistException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TypePaiementServiceImpl implements TypePaiementService {

    private final TypePaiementRepository typePaiementRepository;
    private final TypePaiementMapper typePaiementMapper;

    @Override
    public TypePaiementResponse create(TypePaiementRequest request) {
        typePaiementRepository.findByModeAndStatusDelFalse(request.mode())
                .ifPresent(existing -> {
                    throw new AlreadyExistException("Ce type de paiement existe déjà");
                });

        TypePaiement typePaiement = typePaiementMapper.dtoToModel(request);
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
    public List<TypePaiementResponse> getAll() {
        return typePaiementRepository.findAllByStatusDelFalse()
                .stream()
                .map(typePaiementMapper::modelToDto)
                .toList();
    }

    @Override
    public TypePaiementResponse update(UUID publicId, TypePaiementRequest request) {
        TypePaiement typePaiement = typePaiementRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Type de paiement introuvable"));

        typePaiementRepository.findByModeAndStatusDelFalse(request.mode())
                .ifPresent(existing -> {
                    if (!existing.getPublicId().equals(publicId)) {
                        throw new AlreadyExistException("Un type de paiement avec ce mode existe déjà");
                    }
                });

        typePaiementMapper.dtoToModel(request, typePaiement);
        TypePaiement updated = typePaiementRepository.save(typePaiement);

        return typePaiementMapper.modelToDto(updated);
    }

    @Override
    public void delete(UUID publicId) {
        TypePaiement typePaiement = typePaiementRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Type de paiement introuvable"));

        typePaiement.setStatusDel(true);
        typePaiementRepository.save(typePaiement);
    }
}