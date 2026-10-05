package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.ActeurPastoral;
import com.eyram.dev.church_project_spring.entities.AnneePastorale;
import com.eyram.dev.church_project_spring.entities.MandatPastoral;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.StructurePastorale;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MandatPastoralRepository extends JpaRepository<MandatPastoral, Long> {

    @EntityGraph(attributePaths = {"paroisse", "anneePastorale", "acteurPastoral", "structurePastorale"})
    Optional<MandatPastoral> findByPublicIdAndStatusDelFalse(UUID publicId);

    @EntityGraph(attributePaths = {"paroisse", "anneePastorale", "acteurPastoral", "structurePastorale"})
    List<MandatPastoral> findByParoisseAndAnneePastoraleAndStatusDelFalseOrderByStructurePastorale_OrdreAffichageAscOrdreAffichageAsc(
            Paroisse paroisse,
            AnneePastorale anneePastorale
    );

    boolean existsByStructurePastoraleAndStatusDelFalse(StructurePastorale structurePastorale);

    @Query("""
            SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
            FROM MandatPastoral m
            WHERE m.anneePastorale = :annee
              AND m.acteurPastoral = :acteur
              AND m.structurePastorale = :structure
              AND LOWER(m.fonction) = LOWER(:fonction)
              AND m.statusDel = false
              AND m.dateDebut <= :dateFin
              AND m.dateFin >= :dateDebut
              AND (:excludedPublicId IS NULL OR m.publicId <> :excludedPublicId)
            """)
    boolean existsOverlappingMandate(
            @Param("annee") AnneePastorale annee,
            @Param("acteur") ActeurPastoral acteur,
            @Param("structure") StructurePastorale structure,
            @Param("fonction") String fonction,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin,
            @Param("excludedPublicId") UUID excludedPublicId
    );
}
