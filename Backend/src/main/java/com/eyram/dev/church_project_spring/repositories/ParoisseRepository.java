package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.Localite;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParoisseRepository extends JpaRepository<Paroisse, Long> {
    Optional<Paroisse> findByPublicIdAndStatusDelFalse(UUID publicId);
    List<Paroisse> findAllByStatusDelFalseAndIsActiveTrueOrderByNomAsc();
    List<Paroisse> findAllByStatusDelFalse();
    boolean existsByNomIgnoreCaseAndLocalite_PublicIdAndStatusDelFalse(String nom, UUID localitePublicId);
    boolean existsByNomIgnoreCaseAndLocalite_PublicIdAndStatusDelFalseAndPublicIdNot(
            String nom,
            UUID localitePublicId,
            UUID publicId
    );
    boolean existsByLocaliteAndStatusDelFalse(Localite localite);
    long countByStatusDelFalseAndIsActiveTrue();
}
