package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.PlanSaas;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanSaasRepository extends JpaRepository<PlanSaas, Long> {

    Optional<PlanSaas> findByPublicIdAndStatusDelFalse(UUID publicId);

    Optional<PlanSaas> findByCodeAndStatusDelFalse(String code);

    Optional<PlanSaas> findByCodeAndActifTrueAndStatusDelFalse(String code);

    boolean existsByCodeIgnoreCaseAndStatusDelFalse(String code);

    List<PlanSaas> findByStatusDelFalseOrderByOrdreAffichageAscNomAsc();

    List<PlanSaas> findByActifTrueAndStatusDelFalseOrderByOrdreAffichageAscNomAsc();
}
