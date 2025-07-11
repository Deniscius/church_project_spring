package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.models.Client;
import com.eyram.dev.church_project_spring.repository.ClientRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClientServiceImpl implements ClientService {
    private final ClientRepository clientRepository;

    public ClientServiceImpl(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Override
    public Client create(Client client) {
        if (client==null){
            throw new EntityNotFoundException("Le client n'a pas été reçu");
        }
        Client clients = clientRepository.save(client);
        return clients;
    }

    @Override
    public Client getNom(String nom) {
        if (nom==null){
            throw new EntityNotFoundException("Le nom du client n'a pas été reçu");
        }

        Client clients = clientRepository.findByNom(nom).orElseThrow(()->new EntityNotFoundException("Client non trouvé"));
        return clients;
    }

    @Override
    public Client update(String nom, Client client) {
        Client clientS = getNom(nom);

        if(!clientS.getNom().equals(client.getNom())){
            clientS.setNom(client.getNom());
        }

        if(!clientS.getPrenoms().equals(client.getPrenoms())){
            clientS.setPrenoms(client.getPrenoms());
        }

        if(!clientS.getTel().equals(client.getTel())){
            clientS.setTel(client.getTel());
        }

        clientS=clientRepository.save(clientS);

        return clientS;
    }

    @Override
    public void delete(String nom) {
        Client clientS = getNom(nom);

        clientRepository.delete(clientS);

    }
}
