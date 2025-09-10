package com.eyram.dev.church_project_spring.service;


import com.eyram.dev.church_project_spring.DTO.request.FactureResquest;
import com.eyram.dev.church_project_spring.DTO.response.FactureResponse;
import com.eyram.dev.church_project_spring.entities.Facture;

import java.util.List;
import java.util.UUID;

public interface FactureService {
    FactureResponse create(FactureResquest resquest);
    List<Facture> findAll();
    Facture findByPublicId(UUID publicId);
    FactureResponse update(UUID publicId, FactureResquest factureResquest);
    void delete(UUID publicId);
}
