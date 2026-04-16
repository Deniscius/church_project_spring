package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseRequest;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseResponse;
import com.eyram.dev.church_project_spring.entities.Localite;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.mappers.ParoisseMapper;
import com.eyram.dev.church_project_spring.repositories.LocaliteRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.service.ParoisseService;
import com.eyram.dev.church_project_spring.utils.exception.AlreadyExistException;
import com.eyram.dev.church_project_spring.utils.exception.RequestNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ParoisseServiceImpl implements ParoisseService {

    private final ParoisseRepository paroisseRepository;
    private final LocaliteRepository localiteRepository;
    private final ParoisseMapper paroisseMapper;
    private final TenantAccessService tenantAccessService;

    @Override
    public ParoisseResponse create(ParoisseRequest request) {
        if (request == null) {
            throw new RequestNotFoundException("Requête paroisse non trouvée");
        }

        if (!tenantAccessService.isGlobalUser()) {
            throw new AccessDeniedException("Seul un utilisateur global peut créer une paroisse");
        }

        Localite localite = localiteRepository.findByPublicIdAndStatusDelFalse(request.localitePublicId())
                .orElseThrow(() -> new RequestNotFoundException("Localité non trouvée"));

        boolean exists = paroisseRepository.existsByNomIgnoreCaseAndLocalite_PublicIdAndStatusDelFalse(
                request.nom(),
                request.localitePublicId()
        );

        if (exists) {
            throw new AlreadyExistException("Cette paroisse existe déjà dans cette localité");
        }

        Paroisse paroisse = paroisseMapper.dtoToModel(request);
        paroisse.setPublicId(UUID.randomUUID());
        paroisse.setStatusDel(false);
        paroisse.setIsActive(true);
        paroisse.setLocalite(localite);

        Paroisse savedParoisse = paroisseRepository.save(paroisse);
        return paroisseMapper.modelToDto(savedParoisse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParoisseResponse> getAll() {
        return paroisseRepository.findAllByStatusDelFalse()
                .stream()
                .map(paroisseMapper::modelToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ParoisseResponse getByPublicId(UUID publicId) {
        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new RequestNotFoundException("Paroisse non trouvée"));

        return paroisseMapper.modelToDto(paroisse);
    }

    @Override
    public ParoisseResponse update(UUID publicId, ParoisseRequest request) {
        if (request == null) {
            throw new RequestNotFoundException("Requête paroisse non trouvée");
        }

        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new RequestNotFoundException("Paroisse non trouvée"));

        tenantAccessService.checkParoisseAccess(paroisse);

        Localite localite = localiteRepository.findByPublicIdAndStatusDelFalse(request.localitePublicId())
                .orElseThrow(() -> new RequestNotFoundException("Localité non trouvée"));

        boolean exists = paroisseRepository.existsByNomIgnoreCaseAndLocalite_PublicIdAndStatusDelFalse(
                request.nom(),
                request.localitePublicId()
        );

        if (exists
                && (!paroisse.getNom().equalsIgnoreCase(request.nom())
                || !paroisse.getLocalite().getPublicId().equals(request.localitePublicId()))) {
            throw new AlreadyExistException("Une autre paroisse avec ce nom existe déjà dans cette localité");
        }

        paroisse.setNom(request.nom());
        paroisse.setAdresse(request.adresse());
        paroisse.setEmail(request.email());
        paroisse.setTelephone(request.telephone());
        paroisse.setLocalite(localite);

        Paroisse updatedParoisse = paroisseRepository.save(paroisse);
        return paroisseMapper.modelToDto(updatedParoisse);
    }

    @Override
    public void deleteByPublicId(UUID publicId) {
        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(publicId)
                .orElseThrow(() -> new RequestNotFoundException("Paroisse non trouvée"));

        tenantAccessService.checkParoisseAccess(paroisse);

        paroisse.setIsActive(false);
        paroisse.setStatusDel(true);
        paroisseRepository.save(paroisse);
    }
}