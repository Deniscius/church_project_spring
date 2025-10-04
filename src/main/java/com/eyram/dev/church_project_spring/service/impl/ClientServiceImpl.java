package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.ClientRequest;
import com.eyram.dev.church_project_spring.DTO.response.ClientResponse;
import com.eyram.dev.church_project_spring.entities.Client;
import com.eyram.dev.church_project_spring.mappers.ClientMapper;
import com.eyram.dev.church_project_spring.repository.ClientRepository;
import com.eyram.dev.church_project_spring.service.ClientService;
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
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    public ClientServiceImpl(ClientRepository clientRepository, ClientMapper clientMapper) {
        this.clientRepository = clientRepository;
        this.clientMapper = clientMapper;
    }

    @Override
    public ClientResponse create(ClientRequest request) {
        if (request == null) {
            throw new RequestNotFoundException("La requête du client n'a pas été reçue");
        }
        Client client = clientMapper.dtoToModel(request);
        // sécurité : au cas où le mapper ne l'ait pas positionné
        if (client.getStatusDel() == null) client.setStatusDel(false);

        Client savedClient = clientRepository.save(client);
        return clientMapper.modelToDto(savedClient);
    }

    @Override
    public List<ClientResponse> getNom(String nom) {
        List<Client> clients = clientRepository.findAllByNom(nom);
        if (clients.isEmpty()) {
            throw new EntityNotFoundException("Aucun client trouvé avec le nom : " + nom);
        }
        return clients.stream()
                .filter(c -> Boolean.FALSE.equals(c.getStatusDel()))
                .map(clientMapper::modelToDto)
                .collect(Collectors.toList());
    }

    @Override
    public ClientResponse getByPublicId(UUID publicId) {
        if (publicId == null) {
            throw new TrackingIdNotFoundException("L'identifiant public du client n'a pas été reçu");
        }
        Client client = clientRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Client non trouvé avec l'identifiant public : " + publicId));

        if (Boolean.TRUE.equals(client.getStatusDel())) {
            throw new EntityNotFoundException("Ce client a été supprimé");
        }

        return clientMapper.modelToDto(client);
    }

    @Override
    public List<ClientResponse> getActive() {
        return clientRepository.findAll().stream()
                .filter(c -> Boolean.FALSE.equals(c.getStatusDel()))
                .map(clientMapper::modelToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ClientResponse> getInactive() {
        return clientRepository.findByStatusDelTrue()
                .stream()
                .map(clientMapper::modelToDto)
                .toList();
    }

    @Override
    public List<ClientResponse> getAll() {
        return clientRepository.findAll().stream()
                .map(clientMapper::modelToDto)
                .collect(Collectors.toList());
    }

    @Override
    public ClientResponse update(UUID publicId, ClientRequest request) {
        if (publicId == null) {
            throw new TrackingIdNotFoundException("L'identifiant public du client n'a pas été reçu");
        }
        if (request == null) {
            throw new RequestNotFoundException("La requête du client n'a pas été reçue");
        }

        Client client = clientRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Client non trouvé avec l'identifiant public : " + publicId));

        if (Boolean.TRUE.equals(client.getStatusDel())) {
            throw new EntityNotFoundException("Impossible de mettre à jour un client supprimé");
        }

        if (request.nom() != null && !request.nom().equals(client.getNom())) {
            client.setNom(request.nom());
        }
        if (request.prenoms() != null && !request.prenoms().equals(client.getPrenoms())) {
            client.setPrenoms(request.prenoms());
        }
        if (request.tel() != null && !request.tel().equals(client.getTel())) {
            client.setTel(request.tel());
        }

        client = clientRepository.save(client);
        return clientMapper.modelToDto(client);
    }

    @Override
    public void softDelete(UUID publicId) {
        if (publicId == null) {
            throw new TrackingIdNotFoundException("L'identifiant public du client n'a pas été reçu");
        }
        Client client = clientRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Client non trouvé avec l'identifiant public : " + publicId));

        client.setStatusDel(true);
        clientRepository.save(client);
    }
}
