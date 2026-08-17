package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.PlanSaas;
import com.eyram.dev.church_project_spring.enums.PlanAbonnement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanSaasRepository extends JpaRepository<PlanSaas, Long> {

    Optional<PlanSaas> findByPublicIdAndStatusDelFalse(UUID publicId);

    Optional<PlanSaas> findByCodeAndStatusDelFalse(PlanAbonnement code);

    Optional<PlanSaas> findByCodeAndActifTrueAndStatusDelFalse(PlanAbonnement code);

    List<PlanSaas> findByStatusDelFalseOrderByOrdreAffichageAscNomAsc();

    List<PlanSaas> findByActifTrueAndStatusDelFalseOrderByOrdreAffichageAscNomAsc();
}
