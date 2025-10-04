package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.entities.DetailsPaiement;
import com.eyram.dev.church_project_spring.repository.DetailsPaiementRepository;
import com.eyram.dev.church_project_spring.service.DetailsPaiementService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DetailsPaiementServiceImpl implements DetailsPaiementService {

    private final DetailsPaiementRepository repository;

    @Override
    public DetailsPaiement create(DetailsPaiement detailsPaiement) {
        if (detailsPaiement.getPublicId() == null) {
            detailsPaiement.setPublicId(UUID.randomUUID());
        }
        return repository.save(detailsPaiement);
    }

    @Override
    public DetailsPaiement getByPublicId(UUID publicId) {
        return repository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("DetailsPaiement not found with publicId: " + publicId));
    }

    @Override
    public DetailsPaiement update(UUID publicId, DetailsPaiement detailsPaiement) {
        DetailsPaiement existing = repository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("DetailsPaiement not found with publicId: " + publicId));

        // Mise à jour des champs modifiables
        existing.setMontant(detailsPaiement.getMontant());
        existing.setDateDetailsPaiement(detailsPaiement.getDateDetailsPaiement());
        existing.setStatusDel(detailsPaiement.getStatusDel());

        return repository.save(existing);
    }

    @Override
    public void delete(UUID publicId) {
        DetailsPaiement existing = repository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("DetailsPaiement not found with publicId: " + publicId));
        repository.delete(existing);
    }

    @Override
    public List<DetailsPaiement> getAll() {
        return repository.findAll();
    }

    // Méthodes spécifiques
    public DetailsPaiement getByDate(LocalDate date) {
        return repository.findByDateDetailsPaiement(date)
                .orElseThrow(() -> new EntityNotFoundException("DetailsPaiement not found with date: " + date));
    }

    public DetailsPaiement getByMontant(Double montant) {
        return repository.findByMontant(montant)
                .orElseThrow(() -> new EntityNotFoundException("DetailsPaiement not found with montant: " + montant));
    }

    public List<DetailsPaiement> getByStatus(Boolean statusDel) {
        return repository.findByStatusDel(statusDel);
    }
}
