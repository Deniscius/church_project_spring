package com.eyram.dev.church_project_spring.repository;

import com.eyram.dev.church_project_spring.entities.Paroisse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParoisseRepository extends JpaRepository<Paroisse, Long> {
    Optional<Paroisse> findById(Long id);
    Optional<Paroisse> findByNom(String nom);
    Optional<Paroisse> findByPublicId(UUID publicId);
    List<Paroisse> findAll();
    void deleteByPublicId(UUID publicId);
}
