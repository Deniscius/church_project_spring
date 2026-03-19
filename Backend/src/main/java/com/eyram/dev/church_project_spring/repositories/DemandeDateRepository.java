package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.entities.DemandeDate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DemandeDateRepository extends JpaRepository<DemandeDate, Long> {

    Optional<DemandeDate> findByPublicIdAndStatusDelFalse(UUID publicId);

    List<DemandeDate> findByStatusDelFalse();

    List<DemandeDate> findByDemandeAndStatusDelFalseOrderByOrdreAsc(Demande demande);

    boolean existsByDemandeAndOrdreAndStatusDelFalse(Demande demande, Integer ordre);

    boolean existsByDemandeAndDateCelebrationAndStatusDelFalse(Demande demande, LocalDate dateCelebration);
}