package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.AnneePastorale;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.enums.StatutAnneePastorale;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnneePastoraleRepository extends JpaRepository<AnneePastorale, Long> {

    @EntityGraph(attributePaths = "paroisse")
    Optional<AnneePastorale> findByPublicIdAndStatusDelFalse(UUID publicId);

    @EntityGraph(attributePaths = "paroisse")
    List<AnneePastorale> findByParoisseAndStatusDelFalseOrderByDateDebutDesc(Paroisse paroisse);

    @EntityGraph(attributePaths = "paroisse")
    Optional<AnneePastorale> findByParoisseAndStatutAndStatusDelFalse(
            Paroisse paroisse,
            StatutAnneePastorale statut
    );

    boolean existsByParoisseAndLibelleIgnoreCaseAndStatusDelFalse(Paroisse paroisse, String libelle);

    boolean existsByParoisseAndLibelleIgnoreCaseAndStatusDelFalseAndPublicIdNot(
            Paroisse paroisse,
            String libelle,
            UUID publicId
    );

    @Query("""
            SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
            FROM AnneePastorale a
            WHERE a.paroisse = :paroisse
              AND a.statusDel = false
              AND a.dateDebut <= :dateFin
              AND a.dateFin >= :dateDebut
              AND (:excludedPublicId IS NULL OR a.publicId <> :excludedPublicId)
            """)
    boolean existsOverlappingPeriod(
            @Param("paroisse") Paroisse paroisse,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin,
            @Param("excludedPublicId") UUID excludedPublicId
    );
}
