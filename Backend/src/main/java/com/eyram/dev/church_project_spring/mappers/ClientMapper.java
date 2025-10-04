package com.eyram.dev.church_project_spring.mappers;

import com.eyram.dev.church_project_spring.DTO.request.ClientRequest;
import com.eyram.dev.church_project_spring.DTO.response.ClientResponse;
import com.eyram.dev.church_project_spring.entities.Client;
import com.eyram.dev.church_project_spring.utils.exception.RequestNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class ClientMapper {

    public Client dtoToModel(ClientRequest clientRequest) {
        if (clientRequest == null) {
            throw new RequestNotFoundException("Requête non trouvée");
        }

        Client client = new Client();
        client.setNom(clientRequest.nom());
        client.setPrenoms(clientRequest.prenoms());
        client.setTel(clientRequest.tel());
        client.setStatusDel(false);

        return client;
    }

    public ClientResponse modelToDto(Client entity) {
        if (entity == null) {
            throw new RequestNotFoundException("Client non trouvé");
        }

        return new ClientResponse(
                entity.getPublicId(),
                entity.getNom(),
                entity.getPrenoms(),
                entity.getTel(),
                entity.getStatusDel()
        );
    }
}
