package com.eyram.dev.church_project_spring.repository;

import com.eyram.dev.church_project_spring.entities.Horaire;
import com.eyram.dev.church_project_spring.enums.JourSemaineEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HoraireRepository extends JpaRepository<Horaire, Long> {

    Optional<Horaire> findByPublicId(UUID publicId);
    Optional<Horaire> findByJourSemaine(JourSemaineEnum jourSemaine);
    Optional<Horaire> findByHeure(LocalTime heure);
    boolean existsByJourSemaineAndHeureAndStatusDelFalse(JourSemaineEnum jourSemaine, LocalTime heure);
    List<Horaire> findByStatusDelFalse();
}
