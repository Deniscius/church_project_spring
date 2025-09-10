package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.TypeDemandeRequest;
import com.eyram.dev.church_project_spring.DTO.response.TypeDemandeResponse;
import com.eyram.dev.church_project_spring.entities.TypeDemande;
import com.eyram.dev.church_project_spring.mappers.TypeDemandeMapper;
import com.eyram.dev.church_project_spring.repository.TypeDemandeRepository;
import com.eyram.dev.church_project_spring.service.TypeDemandeService;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TypeDemandeServiceImpl implements TypeDemandeService {

    private final TypeDemandeRepository typeDemandeRepository;
    private final TypeDemandeMapper typeDemandeMapper;

    @Override
    public TypeDemandeResponse create(TypeDemandeRequest request) {

        // Ici, nous vérifions simplement si un type de demande avec le même libellé existe déjà
        if (typeDemandeRepository.findByLibelle(request.libelle()).isPresent()) {
            throw new RuntimeException("Un type de demande avec ce libellé existe déjà");
        }

        TypeDemande typeDemande = typeDemandeMapper.dtoToModel(request);
        TypeDemande savedTypeDemande = typeDemandeRepository.save(typeDemande);

        return typeDemandeMapper.modelToDto(savedTypeDemande);
    }

    @Override
    public TypeDemandeResponse update(UUID publicId, TypeDemandeRequest request) {
        TypeDemande existingTypeDemande = typeDemandeRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("TypeDemande introuvable avec publicId: " + publicId));

        if (!existingTypeDemande.getLibelle().equals(request.libelle()) &&
                typeDemandeRepository.findByLibelle(request.libelle()).isPresent()) {
            throw new RuntimeException("Un type de demande avec ce libellé existe déjà");
        }

        existingTypeDemande.setLibelle(request.libelle());  // Ici, on modifie directement le libellé
        existingTypeDemande.setDescription(request.description());

        TypeDemande updatedTypeDemande = typeDemandeRepository.save(existingTypeDemande);
        return typeDemandeMapper.modelToDto(updatedTypeDemande);
    }

    @Override
    public void delete(UUID publicId) {

        TypeDemande typeDemande = typeDemandeRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("TypeDemande introuvable avec publicId: " + publicId));

        typeDemande.setStatusDel(true);
        typeDemandeRepository.save(typeDemande);
    }

    @Override
    public TypeDemandeResponse getById(UUID publicId) {
        TypeDemande typeDemande = typeDemandeRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("TypeDemande introuvable avec publicId: " + publicId));

        if (Boolean.TRUE.equals(typeDemande.getStatusDel())) {
            throw new ResourceNotFoundException("TypeDemande introuvable avec publicId: " + publicId);
        }

        return typeDemandeMapper.modelToDto(typeDemande);
    }

    @Override
    public List<TypeDemandeResponse> getAll() {
        List<TypeDemande> typeDemandes = typeDemandeRepository.findAll()
                .stream()
                .filter(typeDemande -> Boolean.FALSE.equals(typeDemande.getStatusDel()))
                .collect(Collectors.toList());

        return typeDemandes.stream()
                .map(typeDemandeMapper::modelToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<TypeDemandeResponse> getAllForAdmin() {
        List<TypeDemande> typeDemandes = typeDemandeRepository.findAll();
        return typeDemandes.stream()
                .map(typeDemandeMapper::modelToDto)
                .collect(Collectors.toList());
    }
}
