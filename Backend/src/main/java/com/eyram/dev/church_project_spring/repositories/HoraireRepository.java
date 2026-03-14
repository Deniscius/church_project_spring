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

    List<Horaire> findAllByStatusDelFalse();

    List<Horaire> findAllByParoisseAndStatusDelFalse(Paroisse paroisse);

    List<Horaire> findAllByParoisseAndIsActiveTrueAndStatusDelFalse(Paroisse paroisse);

    boolean existsByParoisseAndJourSemaineAndHeureCelebrationAndStatusDelFalse(
            Paroisse paroisse,
            JourSemaine jourSemaine,
            LocalTime heureCelebration
    );
}