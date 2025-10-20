package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.LocaliteRequest;
import com.eyram.dev.church_project_spring.DTO.response.LocaliteResponse;
import com.eyram.dev.church_project_spring.entities.Localite;
import com.eyram.dev.church_project_spring.repositories.LocaliteRepository;
import com.eyram.dev.church_project_spring.service.LocaliteService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LocaliteServiceImpl implements LocaliteService {

    private final LocaliteRepository localiteRepository;

    public LocaliteServiceImpl(LocaliteRepository localiteRepository) {
        this.localiteRepository = localiteRepository;
    }


    @Override
    public LocaliteResponse create(LocaliteRequest request) {
        if (request.quartier() == null || request.quartier().isEmpty()) {
            throw new IllegalArgumentException("Le champ 'quartier' est obligatoire");
        }
        if (request.ville() == null || request.ville().isEmpty()) {
            throw new IllegalArgumentException("Le champ 'ville' est obligatoire");
        }


        Localite localite = new Localite();
        localite.setPublicId(UUID.randomUUID());
        localite.setQuartier(request.quartier());
        localite.setVille(request.ville());
        localite.setStatusDel(false);

        Localite savedLocalite = localiteRepository.save(localite);
        return new LocaliteResponse(savedLocalite.getPublicId(), savedLocalite.getVille(), savedLocalite.getQuartier(), savedLocalite.getStatusDel());
    }


    @Override
    public LocaliteResponse update(UUID publicId, LocaliteRequest request) {
        Optional<Localite> existingLocalite = localiteRepository.findByPublicId(publicId);
        if (existingLocalite.isEmpty()) {
            throw new RuntimeException("Localité non trouvée avec publicId: " + publicId);
        }

        Localite localite = existingLocalite.get();
        localite.setQuartier(request.quartier());
        localite.setVille(request.ville());

        Localite updatedLocalite = localiteRepository.save(localite);

        return new LocaliteResponse(updatedLocalite.getPublicId(), updatedLocalite.getVille(), updatedLocalite.getQuartier(), updatedLocalite.getStatusDel());
    }

    @Override
    public List<LocaliteResponse> getAll() {

        List<Localite> localites = localiteRepository.findAll();

        return localites.stream().map(localite -> new LocaliteResponse(localite.getPublicId(), localite.getVille(), localite.getQuartier(), localite.getStatusDel())).collect(Collectors.toList());
    }


    @Override
    public LocaliteResponse getByPublicId(UUID publicId) {
        Optional<Localite> localite = localiteRepository.findByPublicId(publicId);
        if (localite.isEmpty()) {
            throw new RuntimeException("Localité non trouvée");
        }

        Localite foundLocalite = localite.get();
        return new LocaliteResponse(foundLocalite.getPublicId(), foundLocalite.getVille(), foundLocalite.getQuartier(), foundLocalite.getStatusDel());
    }

    @Override
    public void delete(UUID publicId) {
        Optional<Localite> localite = localiteRepository.findByPublicId(publicId);
        if (localite.isEmpty()) {
            throw new RuntimeException("Localité non trouvée");
        }

        Localite foundLocalite = localite.get();
        foundLocalite.setStatusDel(true);  // Marquer comme supprimée
        localiteRepository.save(foundLocalite);
    }
}
