package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.LocaliteRequest;
import com.eyram.dev.church_project_spring.DTO.response.LocaliteResponse;
import com.eyram.dev.church_project_spring.entities.Localite;
import com.eyram.dev.church_project_spring.mappers.LocaliteMapper;
import com.eyram.dev.church_project_spring.repositories.LocaliteRepository;
import com.eyram.dev.church_project_spring.service.LocaliteService;
import com.eyram.dev.church_project_spring.utils.exception.RequestNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocaliteServiceImpl implements LocaliteService {

    private final LocaliteRepository localiteRepository;
    private final LocaliteMapper localiteMapper;

    @Override
    public LocaliteResponse create(LocaliteRequest request) {
        if (request == null) {
            throw new RequestNotFoundException("Requête localité non trouvée");
        }

        boolean exists = localiteRepository
                .existsByVilleIgnoreCaseAndQuartierIgnoreCaseAndStatusDelFalse(
                        request.ville(),
                        request.quartier()
                );

        if (exists) {
            throw new RequestNotFoundException("Cette localité existe déjà");
        }

        Localite localite = localiteMapper.dtoToModel(request);
        localite.setPublicId(UUID.randomUUID());
        localite.setStatusDel(false);

        Localite savedLocalite = localiteRepository.save(localite);
        return localiteMapper.modelToDto(savedLocalite);
    }

    @Override
    public LocaliteResponse getByPublicId(UUID publicId) {
        Localite localite = localiteRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new RequestNotFoundException("Localité non trouvée"));

        return localiteMapper.modelToDto(localite);
    }

    @Override
    public List<LocaliteResponse> getAll() {
        return localiteRepository.findAllByStatusDelFalse()
                .stream()
                .map(localiteMapper::modelToDto)
                .toList();
    }

    @Override
    public LocaliteResponse update(UUID publicId, LocaliteRequest request) {
        if (request == null) {
            throw new RequestNotFoundException("Requête localité non trouvée");
        }

        Localite localite = localiteRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new RequestNotFoundException("Localité non trouvée"));

        localite.setVille(request.ville());
        localite.setQuartier(request.quartier());

        Localite updatedLocalite = localiteRepository.save(localite);
        return localiteMapper.modelToDto(updatedLocalite);
    }

    @Override
    public void deleteByPublicId(UUID publicId) {
        Localite localite = localiteRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new RequestNotFoundException("Localité non trouvée"));

        localite.setStatusDel(true);
        localiteRepository.save(localite);
    }
}