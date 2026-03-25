package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.DetailsPaiement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DetailsPaiementRepository extends JpaRepository<DetailsPaiement, Long> {

    Optional<DetailsPaiement> findByPublicIdAndStatusDelFalse(UUID publicId);

    List<DetailsPaiement> findAllByStatusDelFalse();

    Optional<DetailsPaiement> findByFacturePublicId(UUID facturePublicId);

    Optional<DetailsPaiement> findByFacturePublicIdAndStatusDelFalse(UUID facturePublicId);
}