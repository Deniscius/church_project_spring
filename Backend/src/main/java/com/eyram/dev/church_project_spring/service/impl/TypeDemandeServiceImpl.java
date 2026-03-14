package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.TypeDemandeRequest;
import com.eyram.dev.church_project_spring.DTO.response.TypeDemandeResponse;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.TypeDemande;
import com.eyram.dev.church_project_spring.mappers.TypeDemandeMapper;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.repositories.TypeDemandeRepository;
import com.eyram.dev.church_project_spring.service.TypeDemandeService;
import com.eyram.dev.church_project_spring.utils.exception.AlreadyExistException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TypeDemandeServiceImpl implements TypeDemandeService {

    private final TypeDemandeRepository typeDemandeRepository;
    private final ParoisseRepository paroisseRepository;
    private final TypeDemandeMapper typeDemandeMapper;

    @Override
    public TypeDemandeResponse create(TypeDemandeRequest request) {
        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(request.paroissePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));

        if (typeDemandeRepository.existsByLibelleIgnoreCaseAndParoisseAndStatusDelFalse(request.libelle(), paroisse)) {
            throw new AlreadyExistException("Un type de demande avec ce libellé existe déjà dans cette paroisse");
        }

        TypeDemande typeDemande = typeDemandeMapper.toEntity(request);
        typeDemande.setParoisse(paroisse);

        TypeDemande savedTypeDemande = typeDemandeRepository.save(typeDemande);
        return typeDemandeMapper.toResponse(savedTypeDemande);
    }

    @Override
    public TypeDemandeResponse update(UUID publicId, TypeDemandeRequest request) {
        TypeDemande existingTypeDemande = typeDemandeRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Type de demande introuvable"));

        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(request.paroissePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));

        boolean changed =
                !existingTypeDemande.getParoisse().getPublicId().equals(request.paroissePublicId())
                        || !existingTypeDemande.getLibelle().equalsIgnoreCase(request.libelle());

        if (changed && typeDemandeRepository.existsByLibelleIgnoreCaseAndParoisseAndStatusDelFalse(request.libelle(), paroisse)) {
            throw new AlreadyExistException("Un type de demande avec ce libellé existe déjà dans cette paroisse");
        }

        typeDemandeMapper.updateEntityFromRequest(request, existingTypeDemande);
        existingTypeDemande.setParoisse(paroisse);

        TypeDemande updatedTypeDemande = typeDemandeRepository.save(existingTypeDemande);
        return typeDemandeMapper.toResponse(updatedTypeDemande);
    }

    @Override
    public TypeDemandeResponse getByPublicId(UUID publicId) {
        TypeDemande typeDemande = typeDemandeRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Type de demande introuvable"));

        return typeDemandeMapper.toResponse(typeDemande);
    }

    @Override
    public List<TypeDemandeResponse> getAll() {
        return typeDemandeRepository.findAllByStatusDelFalse()
                .stream()
                .map(typeDemandeMapper::toResponse)
                .toList();
    }

    @Override
    public List<TypeDemandeResponse> getByParoisse(UUID paroissePublicId) {
        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));

        return typeDemandeRepository.findAllByParoisseAndStatusDelFalse(paroisse)
                .stream()
                .map(typeDemandeMapper::toResponse)
                .toList();
    }

    @Override
    public void deleteByPublicId(UUID publicId) {
        TypeDemande typeDemande = typeDemandeRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Type de demande introuvable"));

        typeDemande.setStatusDel(true);
        typeDemandeRepository.save(typeDemande);
    }
}