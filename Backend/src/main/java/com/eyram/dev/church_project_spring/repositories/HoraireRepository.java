package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.Horaire;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.enums.JourSemaine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HoraireRepository extends JpaRepository<Horaire, Long> {

    Optional<Horaire> findByPublicIdAndStatusDelFalse(UUID publicId);

    List<Horaire> findByStatusDelFalse();

    List<Horaire> findByParoisseAndStatusDelFalse(Paroisse paroisse);

    List<Horaire> findByParoisseAndIsActiveTrueAndStatusDelFalse(Paroisse paroisse);

    boolean existsByJourSemaineAndHeureCelebrationAndParoisseAndStatusDelFalse(
            JourSemaine jourSemaine,
            LocalTime heureCelebration,
            Paroisse paroisse
    );
}