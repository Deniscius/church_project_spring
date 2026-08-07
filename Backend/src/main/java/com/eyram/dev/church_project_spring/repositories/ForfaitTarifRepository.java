package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.ForfaitTarif;
import com.eyram.dev.church_project_spring.entities.TypeDemande;
import com.eyram.dev.church_project_spring.enums.NatureForfaitEnum;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ForfaitTarifRepository extends JpaRepository<ForfaitTarif, Long> {

    @EntityGraph(attributePaths = {"typeDemande", "joursCelebrationAutorises"})
    Optional<ForfaitTarif> findByPublicIdAndStatusDelFalse(UUID publicId);

    @EntityGraph(attributePaths = {"typeDemande", "joursCelebrationAutorises"})
    List<ForfaitTarif> findByStatusDelFalse();

    @EntityGraph(attributePaths = {"typeDemande", "joursCelebrationAutorises"})
    List<ForfaitTarif> findByTypeDemandeAndStatusDelFalse(TypeDemande typeDemande);

    @EntityGraph(attributePaths = {"typeDemande", "joursCelebrationAutorises"})
    List<ForfaitTarif> findByTypeDemandeAndIsActiveTrueAndStatusDelFalse(TypeDemande typeDemande);

    boolean existsByCodeForfaitAndStatusDelFalse(String codeForfait);

    boolean existsByNomForfaitIgnoreCaseAndTypeDemandeAndStatusDelFalse(
            String nomForfait,
            TypeDemande typeDemande
    );

    boolean existsByTypeDemandeAndNatureForfaitAndStatusDelFalse(
            TypeDemande typeDemande,
            NatureForfaitEnum natureForfait
    );

    boolean existsByTypeDemandeAndNatureForfaitAndStatusDelFalseAndPublicIdNot(
            TypeDemande typeDemande,
            NatureForfaitEnum natureForfait,
            UUID publicId
    );
}
