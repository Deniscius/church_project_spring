package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.FideleRequest;
import com.eyram.dev.church_project_spring.DTO.response.FideleResponse;
import com.eyram.dev.church_project_spring.entities.Fidele;
import com.eyram.dev.church_project_spring.mappers.FideleMapper;
import com.eyram.dev.church_project_spring.repository.FideleRepository;
import com.eyram.dev.church_project_spring.service.FideleService;
import com.eyram.dev.church_project_spring.utils.exception.RequestNotFoundException;
import com.eyram.dev.church_project_spring.utils.exception.TrackingIdNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class FideleServiceImpl implements FideleService {

    private final FideleRepository fideleRepository;
    private final FideleMapper fideleMapper;

    public FideleServiceImpl(FideleRepository fideleRepository, FideleMapper fideleMapper) {
        this.fideleRepository = fideleRepository;
        this.fideleMapper = fideleMapper;
    }

    @Override
    public FideleResponse create(FideleRequest request) {
        if (request == null) {
            throw new RequestNotFoundException("La requête du fidele n'a pas été reçue");
        }
        Fidele fidele = fideleMapper.dtoToModel(request);
        // sécurité : au cas où le mapper ne l'ait pas positionné
        if (fidele.getStatusDel() == null) fidele.setStatusDel(false);

        Fidele savedFidele = fideleRepository.save(fidele);
        return fideleMapper.modelToDto(savedFidele);
    }

    @Override
    public List<FideleResponse> getNom(String nom) {
        List<Fidele> fideles = fideleRepository.findAllByNom(nom);
        if (fideles.isEmpty()) {
            throw new EntityNotFoundException("Aucun client trouvé avec le nom : " + nom);
        }
        return fideles.stream()
                .filter(c -> Boolean.FALSE.equals(c.getStatusDel()))
                .map(fideleMapper::modelToDto)
                .collect(Collectors.toList());
    }

    @Override
    public FideleResponse getByPublicId(UUID publicId) {
        if (publicId == null) {
            throw new TrackingIdNotFoundException("L'identifiant public du fidele n'a pas été reçu");
        }
        Fidele fidele = fideleRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Fidele non trouvé avec l'identifiant public : " + publicId));

        if (Boolean.TRUE.equals(fidele.getStatusDel())) {
            throw new EntityNotFoundException("Ce fidele a été supprimé");
        }

        return fideleMapper.modelToDto(fidele);
    }

    @Override
    public List<FideleResponse> getActive() {
        return fideleRepository.findAll().stream()
                .filter(c -> Boolean.FALSE.equals(c.getStatusDel()))
                .map(fideleMapper::modelToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<FideleResponse> getInactive() {
        return fideleRepository.findByStatusDelTrue()
                .stream()
                .map(fideleMapper::modelToDto)
                .toList();
    }

    @Override
    public List<FideleResponse> getAll() {
        return fideleRepository.findAll().stream()
                .map(fideleMapper::modelToDto)
                .collect(Collectors.toList());
    }

    @Override
    public FideleResponse update(UUID publicId, FideleRequest request) {
        if (publicId == null) {
            throw new TrackingIdNotFoundException("L'identifiant public du fidele n'a pas été reçu");
        }
        if (request == null) {
            throw new RequestNotFoundException("La requête du fidele n'a pas été reçue");
        }

        Fidele fidele = fideleRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Fidele non trouvé avec l'identifiant public : " + publicId));

        if (Boolean.TRUE.equals(fidele.getStatusDel())) {
            throw new EntityNotFoundException("Impossible de mettre à jour un fidele supprimé");
        }

        if (request.nom() != null && !request.nom().equals(fidele.getNom())) {
            fidele.setNom(request.nom());
        }
        if (request.prenoms() != null && !request.prenoms().equals(fidele.getPrenoms())) {
            fidele.setPrenoms(request.prenoms());
        }
        if (request.tel() != null && !request.tel().equals(fidele.getTel())) {
            fidele.setTel(request.tel());
        }

        fidele = fideleRepository.save(fidele);
        return fideleMapper.modelToDto(fidele);
    }

    @Override
    public void softDelete(UUID publicId) {
        if (publicId == null) {
            throw new TrackingIdNotFoundException("L'identifiant public du fidele n'a pas été reçu");
        }
        Fidele fidele = fideleRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Fidele non trouvé avec l'identifiant public : " + publicId));

        fidele.setStatusDel(true);
        fideleRepository.save(fidele);
    }
}
