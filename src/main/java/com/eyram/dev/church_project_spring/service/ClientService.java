package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.ClientRequest;
import com.eyram.dev.church_project_spring.DTO.response.ClientResponse;
import com.eyram.dev.church_project_spring.models.Client;

import java.util.List;
import java.util.UUID;

public interface ClientService {
    ClientResponse create(ClientRequest request);
    ClientResponse getNom(String nom);
    ClientResponse getByPublicId(UUID publicId);
    List<ClientResponse> getAll();
    ClientResponse update(UUID publicId, ClientRequest request);
    void delete(UUID publicId);

}
