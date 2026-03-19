package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.TypePaiement;
import com.eyram.dev.church_project_spring.enums.ModePaiement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TypePaiementRepository extends JpaRepository<TypePaiement, Long> {

    Optional<TypePaiement> findByPublicIdAndStatusDelFalse(UUID publicId);

    Optional<TypePaiement> findByModeAndStatusDelFalse(ModePaiement mode);

    List<TypePaiement> findAllByStatusDelFalse();
}