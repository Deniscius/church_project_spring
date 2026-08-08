package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.Doyenne;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoyenneRepository extends JpaRepository<Doyenne, Long> {

    Optional<Doyenne> findByPublicIdAndStatusDelFalse(UUID publicId);

    /** Ordre officiel du diocèse, le nom ne servant qu'à départager deux rangs égaux. */
    List<Doyenne> findAllByStatusDelFalseOrderByRangAscNomAsc();

    @Query("SELECT COALESCE(MAX(d.rang), 0) FROM Doyenne d WHERE d.statusDel = false")
    int findMaxRang();

    boolean existsByNomIgnoreCaseAndStatusDelFalse(String nom);

    boolean existsByNomIgnoreCaseAndStatusDelFalseAndPublicIdNot(
            String nom,
            UUID publicId
    );
}
