package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.entities.DetailsPaiement;

import java.util.List;
import java.util.UUID;

public interface DetailsPaiementService {
    DetailsPaiement create(DetailsPaiement detailsPaiement);
    DetailsPaiement getByPublicId(UUID publicId);
    DetailsPaiement update(UUID publicId, DetailsPaiement detailsPaiement);
    void delete(UUID publicId);
    List<DetailsPaiement> getAll();

}
