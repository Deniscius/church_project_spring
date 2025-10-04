package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.ClientRequest;
import com.eyram.dev.church_project_spring.DTO.response.ClientResponse;

import java.util.List;
import java.util.UUID;

public interface ClientService {
    ClientResponse create(ClientRequest request);
    List<ClientResponse> getAll();
    List<ClientResponse> getActive();
    List<ClientResponse> getInactive();
    List<ClientResponse> getNom(String nom);
    ClientResponse getByPublicId(UUID publicId);
    ClientResponse update(UUID publicId, ClientRequest request);
    void softDelete(UUID publicId);
}
