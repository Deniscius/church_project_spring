package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.DemandeDateRequest;
import com.eyram.dev.church_project_spring.DTO.response.DemandeDateResponse;
import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.entities.DemandeDate;
import com.eyram.dev.church_project_spring.mappers.DemandeDateMapper;
import com.eyram.dev.church_project_spring.repositories.DemandeDateRepository;
import com.eyram.dev.church_project_spring.repositories.DemandeRepository;
import com.eyram.dev.church_project_spring.service.DemandeDateService;
import com.eyram.dev.church_project_spring.utils.exception.AlreadyExistException;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DemandeDateServiceImpl implements DemandeDateService {

    private final DemandeDateRepository demandeDateRepository;
    private final DemandeRepository demandeRepository;
    private final DemandeDateMapper demandeDateMapper;

    @Override
    public DemandeDateResponse create(DemandeDateRequest request) {

        Demande demande = demandeRepository.findByPublicIdAndStatusDelFalse(request.demandePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        if (demandeDateRepository.existsByDemandeAndOrdreAndStatusDelFalse(demande, request.ordre())) {
            throw new AlreadyExistException("Cet ordre existe déjà pour cette demande");
        }

        if (demandeDateRepository.existsByDemandeAndDateCelebrationAndStatusDelFalse(demande, request.dateCelebration())) {
            throw new AlreadyExistException("Cette date existe déjà pour cette demande");
        }

        DemandeDate demandeDate = demandeDateMapper.dtoToModel(request);
        demandeDate.setDemande(demande);

        DemandeDate savedDemandeDate = demandeDateRepository.save(demandeDate);
        return demandeDateMapper.modelToDto(savedDemandeDate);
    }

    @Override
    public DemandeDateResponse update(UUID publicId, DemandeDateRequest request) {

        DemandeDate existingDemandeDate = demandeDateRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Date de demande introuvable"));

        Demande demande = demandeRepository.findByPublicIdAndStatusDelFalse(request.demandePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        boolean ordreChanged =
                !existingDemandeDate.getOrdre().equals(request.ordre()) ||
                        !existingDemandeDate.getDemande().getPublicId().equals(request.demandePublicId());

        if (ordreChanged && demandeDateRepository.existsByDemandeAndOrdreAndStatusDelFalse(demande, request.ordre())) {
            throw new AlreadyExistException("Cet ordre existe déjà pour cette demande");
        }

        boolean dateChanged =
                !existingDemandeDate.getDateCelebration().equals(request.dateCelebration()) ||
                        !existingDemandeDate.getDemande().getPublicId().equals(request.demandePublicId());

        if (dateChanged && demandeDateRepository.existsByDemandeAndDateCelebrationAndStatusDelFalse(demande, request.dateCelebration())) {
            throw new AlreadyExistException("Cette date existe déjà pour cette demande");
        }

        demandeDateMapper.updateEntityFromDto(request, existingDemandeDate);
        existingDemandeDate.setDemande(demande);

        DemandeDate updatedDemandeDate = demandeDateRepository.save(existingDemandeDate);
        return demandeDateMapper.modelToDto(updatedDemandeDate);
    }

    @Override
    public DemandeDateResponse getByPublicId(UUID publicId) {
        DemandeDate demandeDate = demandeDateRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Date de demande introuvable"));

        return demandeDateMapper.modelToDto(demandeDate);
    }

    @Override
    public List<DemandeDateResponse> getAll() {
        return demandeDateRepository.findByStatusDelFalse()
                .stream()
                .map(demandeDateMapper::modelToDto)
                .toList();
    }

    @Override
    public List<DemandeDateResponse> getByDemande(UUID demandePublicId) {
        Demande demande = demandeRepository.findByPublicIdAndStatusDelFalse(demandePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        return demandeDateRepository.findByDemandeAndStatusDelFalseOrderByOrdreAsc(demande)
                .stream()
                .map(demandeDateMapper::modelToDto)
                .toList();
    }

    @Override
    public void deleteByPublicId(UUID publicId) {
        DemandeDate demandeDate = demandeDateRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Date de demande introuvable"));

        demandeDate.setStatusDel(true);
        demandeDateRepository.save(demandeDate);
    }
}