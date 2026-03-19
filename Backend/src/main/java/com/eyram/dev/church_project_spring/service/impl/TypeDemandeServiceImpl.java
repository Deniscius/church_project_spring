package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.TypeDemandeRequest;
import com.eyram.dev.church_project_spring.DTO.response.TypeDemandeResponse;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.TypeDemande;
import com.eyram.dev.church_project_spring.enums.TypeDemandeEnum;
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

        boolean exists = typeDemandeRepository.existsByLibelleIgnoreCaseAndParoisseAndTypeDemandeEnumAndStatusDelFalse(
                request.libelle(),
                paroisse,
                request.typeDemandeEnum()
        );

        if (exists) {
            throw new AlreadyExistException("Ce type de demande existe déjà pour cette paroisse");
        }

        TypeDemande typeDemande = typeDemandeMapper.dtoToModel(request);
        typeDemande.setParoisse(paroisse);

        TypeDemande savedTypeDemande = typeDemandeRepository.save(typeDemande);
        return typeDemandeMapper.modelToDto(savedTypeDemande);
    }

    @Override
    public TypeDemandeResponse update(UUID publicId, TypeDemandeRequest request) {

        TypeDemande existingTypeDemande = typeDemandeRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Type de demande introuvable"));

        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(request.paroissePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));

        boolean dataChanged =
                !existingTypeDemande.getLibelle().equalsIgnoreCase(request.libelle()) ||
                        !existingTypeDemande.getParoisse().getPublicId().equals(request.paroissePublicId()) ||
                        !existingTypeDemande.getTypeDemandeEnum().equals(request.typeDemandeEnum());

        if (dataChanged) {
            boolean exists = typeDemandeRepository.existsByLibelleIgnoreCaseAndParoisseAndTypeDemandeEnumAndStatusDelFalse(
                    request.libelle(),
                    paroisse,
                    request.typeDemandeEnum()
            );

            if (exists) {
                throw new AlreadyExistException("Ce type de demande existe déjà pour cette paroisse");
            }
        }

        typeDemandeMapper.updateEntityFromDto(request, existingTypeDemande);
        existingTypeDemande.setParoisse(paroisse);

        TypeDemande updatedTypeDemande = typeDemandeRepository.save(existingTypeDemande);
        return typeDemandeMapper.modelToDto(updatedTypeDemande);
    }

    @Override
    public TypeDemandeResponse getByPublicId(UUID publicId) {
        TypeDemande typeDemande = typeDemandeRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Type de demande introuvable"));

        return typeDemandeMapper.modelToDto(typeDemande);
    }

    @Override
    public List<TypeDemandeResponse> getAll() {
        return typeDemandeRepository.findByStatusDelFalse()
                .stream()
                .map(typeDemandeMapper::modelToDto)
                .toList();
    }

    @Override
    public List<TypeDemandeResponse> getByParoisse(UUID paroissePublicId) {
        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));

        return typeDemandeRepository.findByParoisseAndStatusDelFalse(paroisse)
                .stream()
                .map(typeDemandeMapper::modelToDto)
                .toList();
    }

    @Override
    public List<TypeDemandeResponse> getByTypeDemandeEnum(TypeDemandeEnum typeDemandeEnum) {
        return typeDemandeRepository.findByTypeDemandeEnumAndStatusDelFalse(typeDemandeEnum)
                .stream()
                .map(typeDemandeMapper::modelToDto)
                .toList();
    }

    @Override
    public List<TypeDemandeResponse> getByParoisseAndTypeDemandeEnum(UUID paroissePublicId, TypeDemandeEnum typeDemandeEnum) {
        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));

        return typeDemandeRepository.findByParoisseAndTypeDemandeEnumAndStatusDelFalse(paroisse, typeDemandeEnum)
                .stream()
                .map(typeDemandeMapper::modelToDto)
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