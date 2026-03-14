package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.TypeDemande;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TypeDemandeRepository extends JpaRepository<TypeDemande, Long> {

    Optional<TypeDemande> findByPublicIdAndStatusDelFalse(UUID publicId);

    List<TypeDemande> findAllByStatusDelFalse();

    List<TypeDemande> findAllByParoisseAndStatusDelFalse(Paroisse paroisse);

    List<TypeDemande> findAllByParoisseAndIsActiveTrueAndStatusDelFalse(Paroisse paroisse);

    boolean existsByLibelleIgnoreCaseAndParoisseAndStatusDelFalse(String libelle, Paroisse paroisse);
}