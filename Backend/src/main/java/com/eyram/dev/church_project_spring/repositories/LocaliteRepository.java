package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.Localite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LocaliteRepository extends JpaRepository<Localite, Long> {

    Optional<Localite> findByPublicIdAndStatusDelFalse(UUID publicId);

    List<Localite> findAllByStatusDelFalse();

    boolean existsByVilleIgnoreCaseAndQuartierIgnoreCaseAndStatusDelFalse(String ville, String quartier);
}