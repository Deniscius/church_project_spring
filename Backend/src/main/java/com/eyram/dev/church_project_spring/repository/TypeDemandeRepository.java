package com.eyram.dev.church_project_spring.repository;

import com.eyram.dev.church_project_spring.entities.TypeDemande;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TypeDemandeRepository extends JpaRepository<TypeDemande, Long> {
    Optional<TypeDemande> findByLibelle(String libelle);
    Optional<TypeDemande> findById(Long id);
    Optional<TypeDemande> findByDescription(String description);
    Optional<TypeDemande> findByPublicId(UUID publicId);

}
