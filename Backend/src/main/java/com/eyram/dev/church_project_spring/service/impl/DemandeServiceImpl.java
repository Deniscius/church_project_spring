package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.DemandeRequest;
import com.eyram.dev.church_project_spring.DTO.response.DemandeResponse;
import com.eyram.dev.church_project_spring.entities.Client;
import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.enums.StatusValidationEnum;
import com.eyram.dev.church_project_spring.mappers.DemandeMapper;
import com.eyram.dev.church_project_spring.repository.ClientRepository;
import com.eyram.dev.church_project_spring.repository.DemandeRepository;
import com.eyram.dev.church_project_spring.service.DemandeService;
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
public class DemandeServiceImpl implements DemandeService {

    private final DemandeRepository demandeRepository;
    private final ClientRepository clientRepository;
    private final DemandeMapper demandeMapper;

    public DemandeServiceImpl(DemandeRepository demandeRepository, ClientRepository clientRepository, DemandeMapper demandeMapper) {
        this.demandeRepository = demandeRepository;
        this.clientRepository = clientRepository;
        this.demandeMapper = demandeMapper;
    }

    @Override
    public DemandeResponse create(DemandeRequest request) {
        if (request == null) {
            throw new RequestNotFoundException("La requête de la demande n'a pas été reçue");
        }

        Client client = clientRepository.findByPublicId(request.clientPublicId()).orElseThrow(() -> new EntityNotFoundException("Client non trouvé avec l'identifiant public : " + request.clientPublicId()));

        Demande demande = demandeMapper.dtoToModel(request);
        demande.setClient(client);
        demande.setStatusDel(false);
        demande.setStatusValidationEnum(StatusValidationEnum.EN_ATTENTE);

        Demande savedDemande = demandeRepository.save(demande);
        return demandeMapper.modelToDto(savedDemande);
    }

    @Override
    public DemandeResponse getByPublicId(UUID publicId) {
        Demande demande = getByPublicIdEntity(publicId);
        return demandeMapper.modelToDto(demande);
    }

    @Override
    public List<DemandeResponse> getAll() {
        return demandeRepository.findAll().stream().filter(d -> !Boolean.TRUE.equals(d.getStatusDel())).map(demandeMapper::modelToDto).collect(Collectors.toList());
    }

    @Override
    public List<DemandeResponse> getAllByClient(UUID clientPublicId) {
        Client client = clientRepository.findByPublicId(clientPublicId).orElseThrow(() -> new EntityNotFoundException("Client non trouvé avec l'identifiant public : " + clientPublicId));

        return demandeRepository.findByClient(client).stream().filter(d -> !Boolean.TRUE.equals(d.getStatusDel())).map(demandeMapper::modelToDto).collect(Collectors.toList());
    }

    @Override
    public List<DemandeResponse> getAllValidated(boolean statusValidation) {
        return List.of();
    }

    @Override
    public List<DemandeResponse> getAllByStatus(StatusValidationEnum status) {
        return demandeRepository.findAll().stream().filter(d -> !Boolean.TRUE.equals(d.getStatusDel())).filter(d -> d.getStatusValidationEnum() == status).map(demandeMapper::modelToDto).collect(Collectors.toList());
    }

    @Override
    public DemandeResponse update(UUID publicId, DemandeRequest request) {
        Demande demande = getByPublicIdEntity(publicId);

        if (!demande.getIntention().equals(request.intention())) {
            demande.setIntention(request.intention());
        }

        if (!demande.getDateDemande().equals(request.dateDemande())) {
            demande.setDateDemande(request.dateDemande());
        }

        Demande updated = demandeRepository.save(demande);
        return demandeMapper.modelToDto(updated);
    }

    @Override
    public DemandeResponse validateDemande(UUID publicId) {
        Demande demande = getByPublicIdEntity(publicId);

        if (demande.getStatusValidationEnum() == StatusValidationEnum.VALIDEE) {
            throw new IllegalStateException("Demande déjà validée");
        }

        demande.setStatusValidationEnum(StatusValidationEnum.VALIDEE);
        Demande saved = demandeRepository.save(demande);
        return demandeMapper.modelToDto(saved);
    }

    @Override
    public DemandeResponse cancelDemande(UUID publicId) {
        Demande demande = getByPublicIdEntity(publicId);

        if (demande.getStatusValidationEnum() != StatusValidationEnum.VALIDEE) {
            throw new IllegalStateException("Seules les demandes validées peuvent être annulées");
        }

        demande.setStatusValidationEnum(StatusValidationEnum.EN_ATTENTE);
        Demande saved = demandeRepository.save(demande);
        return demandeMapper.modelToDto(saved);
    }

    @Override
    public void delete(UUID publicId) {
        Demande demande = getByPublicIdEntity(publicId);
        demande.setStatusDel(true);
        demandeRepository.save(demande);
    }

    private Demande getByPublicIdEntity(UUID publicId) {
        if (publicId == null) {
            throw new TrackingIdNotFoundException("L'identifiant public de la demande n'a pas été reçu");
        }

        Demande demande = demandeRepository.findByPublicId(publicId).orElseThrow(() -> new EntityNotFoundException("Demande non trouvée avec l'identifiant public : " + publicId));

        if (Boolean.TRUE.equals(demande.getStatusDel())) {
            throw new EntityNotFoundException("Cette demande a été supprimée");
        }

        return demande;
    }
}
