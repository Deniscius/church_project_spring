package com.eyram.dev.church_project_spring.repository;

import com.eyram.dev.church_project_spring.entities.Localite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LocaliteRepository extends JpaRepository<Localite, Long> {
    List<Localite> findAll();
    Optional<Localite> findByPublicId(UUID publicId);
    Optional<Localite> findByQuartier(String quartier);
    Optional<Localite> findByVille(String ville);
}
