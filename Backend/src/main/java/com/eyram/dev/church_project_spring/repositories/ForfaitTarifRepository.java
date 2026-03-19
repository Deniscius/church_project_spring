package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.ForfaitTarif;
import com.eyram.dev.church_project_spring.entities.TypeDemande;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ForfaitTarifRepository extends JpaRepository<ForfaitTarif, Long> {

    Optional<ForfaitTarif> findByPublicIdAndStatusDelFalse(UUID publicId);

    List<ForfaitTarif> findByStatusDelFalse();

    List<ForfaitTarif> findByTypeDemandeAndStatusDelFalse(TypeDemande typeDemande);

    List<ForfaitTarif> findByTypeDemandeAndIsActiveTrueAndStatusDelFalse(TypeDemande typeDemande);

    boolean existsByCodeForfaitAndStatusDelFalse(String codeForfait);

    boolean existsByNomForfaitIgnoreCaseAndTypeDemandeAndStatusDelFalse(
            String nomForfait,
            TypeDemande typeDemande
    );
}