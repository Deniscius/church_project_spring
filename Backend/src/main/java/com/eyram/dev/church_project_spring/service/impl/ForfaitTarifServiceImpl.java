package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.ForfaitTarifRequest;
import com.eyram.dev.church_project_spring.DTO.response.ForfaitTarifResponse;
import com.eyram.dev.church_project_spring.entities.ForfaitTarif;
import com.eyram.dev.church_project_spring.entities.TypeDemande;
import com.eyram.dev.church_project_spring.mappers.ForfaitTarifMapper;
import com.eyram.dev.church_project_spring.repositories.ForfaitTarifRepository;
import com.eyram.dev.church_project_spring.repositories.TypeDemandeRepository;
import com.eyram.dev.church_project_spring.service.ForfaitTarifService;
import com.eyram.dev.church_project_spring.utils.exception.AlreadyExistException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ForfaitTarifServiceImpl implements ForfaitTarifService {

    private final ForfaitTarifRepository forfaitTarifRepository;
    private final TypeDemandeRepository typeDemandeRepository;
    private final ForfaitTarifMapper forfaitTarifMapper;

    @Override
    public ForfaitTarifResponse create(ForfaitTarifRequest request) {

        TypeDemande typeDemande = typeDemandeRepository.findByPublicIdAndStatusDelFalse(request.typeDemandePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Type de demande introuvable"));

        if (forfaitTarifRepository.existsByCodeForfaitAndStatusDelFalse(request.codeForfait())) {
            throw new AlreadyExistException("Un forfait avec ce code existe déjà");
        }

        if (forfaitTarifRepository.existsByNomForfaitIgnoreCaseAndTypeDemandeAndStatusDelFalse(
                request.nomForfait(), typeDemande)) {
            throw new AlreadyExistException("Un forfait avec ce nom existe déjà pour ce type de demande");
        }

        ForfaitTarif forfaitTarif = forfaitTarifMapper.dtoToModel(request);
        forfaitTarif.setTypeDemande(typeDemande);

        ForfaitTarif savedForfaitTarif = forfaitTarifRepository.save(forfaitTarif);
        return forfaitTarifMapper.modelToDto(savedForfaitTarif);
    }

    @Override
    public ForfaitTarifResponse update(UUID publicId, ForfaitTarifRequest request) {

        ForfaitTarif existingForfaitTarif = forfaitTarifRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Forfait tarif introuvable"));

        TypeDemande typeDemande = typeDemandeRepository.findByPublicIdAndStatusDelFalse(request.typeDemandePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Type de demande introuvable"));

        if (!existingForfaitTarif.getCodeForfait().equals(request.codeForfait())
                && forfaitTarifRepository.existsByCodeForfaitAndStatusDelFalse(request.codeForfait())) {
            throw new AlreadyExistException("Un forfait avec ce code existe déjà");
        }

        boolean changed =
                !existingForfaitTarif.getNomForfait().equalsIgnoreCase(request.nomForfait()) ||
                        !existingForfaitTarif.getTypeDemande().getPublicId().equals(request.typeDemandePublicId());

        if (changed && forfaitTarifRepository.existsByNomForfaitIgnoreCaseAndTypeDemandeAndStatusDelFalse(
                request.nomForfait(), typeDemande)) {
            throw new AlreadyExistException("Un forfait avec ce nom existe déjà pour ce type de demande");
        }

        forfaitTarifMapper.updateEntityFromDto(request, existingForfaitTarif);
        existingForfaitTarif.setTypeDemande(typeDemande);

        ForfaitTarif updatedForfaitTarif = forfaitTarifRepository.save(existingForfaitTarif);
        return forfaitTarifMapper.modelToDto(updatedForfaitTarif);
    }

    @Override
    public ForfaitTarifResponse getByPublicId(UUID publicId) {
        ForfaitTarif forfaitTarif = forfaitTarifRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Forfait tarif introuvable"));

        return forfaitTarifMapper.modelToDto(forfaitTarif);
    }

    @Override
    public List<ForfaitTarifResponse> getAll() {
        return forfaitTarifRepository.findByStatusDelFalse()
                .stream()
                .map(forfaitTarifMapper::modelToDto)
                .toList();
    }

    @Override
    public List<ForfaitTarifResponse> getByTypeDemande(UUID typeDemandePublicId) {
        TypeDemande typeDemande = typeDemandeRepository.findByPublicIdAndStatusDelFalse(typeDemandePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Type de demande introuvable"));

        return forfaitTarifRepository.findByTypeDemandeAndStatusDelFalse(typeDemande)
                .stream()
                .map(forfaitTarifMapper::modelToDto)
                .toList();
    }

    @Override
    public List<ForfaitTarifResponse> getActiveByTypeDemande(UUID typeDemandePublicId) {
        TypeDemande typeDemande = typeDemandeRepository.findByPublicIdAndStatusDelFalse(typeDemandePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Type de demande introuvable"));

        return forfaitTarifRepository.findByTypeDemandeAndIsActiveTrueAndStatusDelFalse(typeDemande)
                .stream()
                .map(forfaitTarifMapper::modelToDto)
                .toList();
    }

    @Override
    public void deleteByPublicId(UUID publicId) {
        ForfaitTarif forfaitTarif = forfaitTarifRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Forfait tarif introuvable"));

        forfaitTarif.setStatusDel(true);
        forfaitTarifRepository.save(forfaitTarif);
    }
}