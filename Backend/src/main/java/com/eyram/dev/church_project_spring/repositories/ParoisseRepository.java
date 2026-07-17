package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.Doyenne;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParoisseRepository extends JpaRepository<Paroisse, Long> {
    Optional<Paroisse> findByPublicIdAndStatusDelFalse(UUID publicId);
    List<Paroisse> findAllByStatusDelFalseAndIsActiveTrueOrderByNomAsc();
    List<Paroisse> findAllByStatusDelFalse();
    boolean existsByNomIgnoreCaseAndDoyenne_PublicIdAndStatusDelFalse(String nom, UUID doyennePublicId);
    boolean existsByNomIgnoreCaseAndDoyenne_PublicIdAndStatusDelFalseAndPublicIdNot(
            String nom,
            UUID doyennePublicId,
            UUID publicId
    );
    boolean existsByDoyenneAndStatusDelFalse(Doyenne doyenne);
    long countByStatusDelFalseAndIsActiveTrue();
}
