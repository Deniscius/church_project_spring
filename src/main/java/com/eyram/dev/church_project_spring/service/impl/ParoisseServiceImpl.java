package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseRequest;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseResponse;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.mappers.ParoisseMapper;
import com.eyram.dev.church_project_spring.repository.ParoisseRepository;
import com.eyram.dev.church_project_spring.service.ParoisseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ParoisseServiceImpl implements ParoisseService {

    private final ParoisseRepository paroisseRepository;
    private final ParoisseMapper paroisseMapper;

    @Autowired
    public ParoisseServiceImpl(ParoisseRepository paroisseRepository, ParoisseMapper paroisseMapper) {
        this.paroisseRepository = paroisseRepository;
        this.paroisseMapper = paroisseMapper;
    }

    @Override
    public ParoisseResponse create(ParoisseRequest request) {
        Optional<Paroisse> existingParoisse = paroisseRepository.findByNom(request.nom());
        if (existingParoisse.isPresent()) {
            throw new RuntimeException("La paroisse existe déjà");
        }

        Paroisse paroisse = new Paroisse();
        paroisse.setNom(request.nom());
        paroisse.setTel(request.tel());
        paroisse.setStatusDel(false);

        Paroisse savedParoisse = paroisseRepository.save(paroisse);

        return new ParoisseResponse(savedParoisse.getPublicId(), savedParoisse.getNom(), savedParoisse.getTel(), savedParoisse.getStatusDel());
    }


    @Override
    public List<ParoisseResponse> list() {

        List<Paroisse> paroisses = paroisseRepository.findAll();
        return paroisses.stream()
                .map(paroisseMapper::modelToDto)
                .collect(Collectors.toList());
    }

    @Override
    public ParoisseResponse update(ParoisseRequest request) {

        Optional<Paroisse> existingParoisse = paroisseRepository.findByPublicId(request.publicId());
        if (existingParoisse.isEmpty()) {
            throw new RuntimeException("Paroisse non trouvée");
        }


        Paroisse paroisse = existingParoisse.get();
        paroisse.setNom(request.nom());


        Paroisse updatedParoisse = paroisseRepository.save(paroisse);


        return paroisseMapper.modelToDto(updatedParoisse);
    }

    @Override
    public ParoisseResponse deleteByName(String name) {
        Optional<Paroisse> paroisse = paroisseRepository.findByNom(name);
        if (paroisse.isEmpty()) {
            throw new RuntimeException("Paroisse non trouvée");
        }
        paroisseRepository.delete(paroisse.get());

        return paroisseMapper.modelToDto(paroisse.get());
    }

    @Override
    public ParoisseResponse deleteByPublicId(UUID publicId) {
        Optional<Paroisse> paroisse = paroisseRepository.findByPublicId(publicId);
        if (paroisse.isEmpty()) {
            throw new RuntimeException("Paroisse non trouvée");
        }
        paroisseRepository.delete(paroisse.get());

        return paroisseMapper.modelToDto(paroisse.get());
    }

    @Override
    public ParoisseResponse getByPublicId(UUID publicId) {
        Paroisse paroisse = paroisseRepository.findByPublicId(publicId)
                .orElseThrow(() -> new RuntimeException(
                        "Paroisse introuvable avec publicId: " + publicId
                ));

        if (Boolean.TRUE.equals(paroisse.getStatusDel())) {
            throw new RuntimeException(
                    "Paroisse introuvable avec publicId: " + publicId
            );
        }

        ParoisseResponse response = new ParoisseResponse(
                paroisse.getPublicId(),
                paroisse.getNom(),
                paroisse.getTel(),
                paroisse.getStatusDel()
        );

        return response;
    }

}
