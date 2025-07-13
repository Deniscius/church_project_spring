package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.ClientRequest;
import com.eyram.dev.church_project_spring.DTO.response.ClientResponse;
import com.eyram.dev.church_project_spring.models.Client;
import com.eyram.dev.church_project_spring.utils.exception.RequestNotFoundException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ClientMapper {

    public Client dtoToModel(ClientRequest clientRequest) {
        if (clientRequest == null) {
            throw new RequestNotFoundException("Requête non trouvée");
        }

        Client client = new Client();
        client.setPublicId(UUID.randomUUID());
        client.setNom(clientRequest.nom());
        client.setPrenoms(clientRequest.prenoms());
        client.setTel(clientRequest.tel());
        client.setStatus(true);

        return client;

    }

    public ClientResponse modelToDto(Client client) {
        if (client ==null) {
            throw new RequestNotFoundException("Client non trouvé");
        }

        ClientResponse clientResponse = new ClientResponse(client.getPublicId(), client.getNom(), client.getPrenoms(), client.getTel(),client.isStatus());
        return clientResponse;
    }

}
