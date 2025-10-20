package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.DetailsPaiement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DetailsPaiementRepository extends JpaRepository<DetailsPaiement, Long> {
    Optional<DetailsPaiement> findById(Long id);
    Optional<DetailsPaiement> findByPublicId(UUID publicId);
    Optional<DetailsPaiement> findByDateDetailsPaiement(LocalDate dateDetailsPaiement);
    Optional<DetailsPaiement> findByMontant(Double montant);
    List<DetailsPaiement> findByStatusDel(Boolean statusDel);
}
