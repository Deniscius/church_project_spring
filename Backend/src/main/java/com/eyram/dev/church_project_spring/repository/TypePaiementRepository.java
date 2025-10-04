package com.eyram.dev.church_project_spring.repository;

import com.eyram.dev.church_project_spring.entities.TypePaiement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TypePaiementRepository extends JpaRepository<TypePaiement, Long> {
    Optional<TypePaiement> findByPublicId(UUID publicId);
    Optional<TypePaiement> findByLibelle(String libelle);
    List<TypePaiement> findByStatusDel(Boolean statusDel);
}
