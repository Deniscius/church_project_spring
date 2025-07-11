package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.models.Client;

public interface ClientService {
    Client create(Client client);
    Client getNom(String nom);
    Client update(String nom, Client client);
    void delete(String nom);
}
