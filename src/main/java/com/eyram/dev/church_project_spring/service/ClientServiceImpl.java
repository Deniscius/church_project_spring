package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.ClientRequest;
import com.eyram.dev.church_project_spring.DTO.response.ClientResponse;
import com.eyram.dev.church_project_spring.mappers.ClientMapper;
import com.eyram.dev.church_project_spring.models.Client;
import com.eyram.dev.church_project_spring.repository.ClientRepository;
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
        Client savedClient = clientRepository.save(client);
        return clientMapper.modelToDto(savedClient);
    }

    @Override
    public ClientResponse getNom(String nom) {
        return clientMapper.modelToDto(clientRepository.findByNom(nom).orElseThrow(() -> new EntityNotFoundException("Client non trouvé avec le nom : " + nom)));
    }

    @Override
    public ClientResponse getByPublicId(UUID publicId) {
        if (publicId == null) {
            throw new TrackingIdNotFoundException("L'identifiant public du client n'a pas été reçu");
        }
        return clientMapper.modelToDto(clientRepository.findByPublicId(publicId).orElseThrow(()->new EntityNotFoundException("Client non trouvé avec l'identifiant public : " + publicId)));

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
                .orElseThrow(() -> new EntityNotFoundException("Client non trouvé avec l'identifiant public : " + publicId));
        if (!client.getNom().equals(request.nom())) {
            client.setNom(request.nom());
        }
        if (!client.getPrenoms().equals(request.prenoms())) {
            client.setPrenoms(request.prenoms());
        }
        if (!client.getTel().equals(request.tel())) {
            client.setTel(request.tel());
        }

        client = clientRepository.save(client);

        return clientMapper.modelToDto(client);
    }


    @Override
    public void delete(UUID publicId) {
        if (publicId == null) {
            throw new TrackingIdNotFoundException("L'identifiant public du client n'a pas été reçu");
        }
        Client client = clientRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("Client non trouvé avec l'identifiant public : " + publicId));
            client.setStatus(false);
    }

}
