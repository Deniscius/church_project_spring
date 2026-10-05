package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.ActeurPastoral;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActeurPastoralRepository extends JpaRepository<ActeurPastoral, Long> {

    @EntityGraph(attributePaths = "paroisse")
    Optional<ActeurPastoral> findByPublicIdAndStatusDelFalse(UUID publicId);

    @EntityGraph(attributePaths = "paroisse")
    @Query("""
            SELECT a
            FROM ActeurPastoral a
            WHERE a.paroisse = :paroisse
              AND a.statusDel = false
              AND (:actif IS NULL OR a.actif = :actif)
              AND (
                    :recherche IS NULL
                    OR LOWER(CONCAT(CONCAT(a.prenoms, ' '), a.nom)) LIKE :recherche
                    OR LOWER(CONCAT(CONCAT(a.nom, ' '), a.prenoms)) LIKE :recherche
                    OR LOWER(COALESCE(a.email, '')) LIKE :recherche
                    OR COALESCE(a.telephone, '') LIKE :recherche
              )
            ORDER BY a.nom ASC, a.prenoms ASC
            """)
    List<ActeurPastoral> searchByParoisse(
            @Param("paroisse") Paroisse paroisse,
            @Param("actif") Boolean actif,
            @Param("recherche") String recherche
    );
}
