package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.Doyenne;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoyenneRepository extends JpaRepository<Doyenne, Long> {

    Optional<Doyenne> findByPublicIdAndStatusDelFalse(UUID publicId);

    List<Doyenne> findAllByStatusDelFalseOrderByNomAsc();

    boolean existsByNomIgnoreCaseAndStatusDelFalse(String nom);

    boolean existsByNomIgnoreCaseAndStatusDelFalseAndPublicIdNot(
            String nom,
            UUID publicId
    );
}
