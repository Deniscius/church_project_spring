package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.Horaire;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.enums.JourSemaine;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HoraireRepository extends JpaRepository<Horaire, Long> {

    @EntityGraph(attributePaths = {"paroisse"})
    Optional<Horaire> findByPublicIdAndStatusDelFalse(UUID publicId);

    @EntityGraph(attributePaths = {"paroisse"})
    List<Horaire> findByStatusDelFalse();

    @EntityGraph(attributePaths = {"paroisse"})
    List<Horaire> findByParoisseAndStatusDelFalse(Paroisse paroisse);

    @EntityGraph(attributePaths = {"paroisse"})
    List<Horaire> findByParoisseAndIsActiveTrueAndStatusDelFalse(Paroisse paroisse);

    /**
     * Horaires actifs des paroisses accessibles (abonnement ACTIVE / EN_TOLERANCE).
     * Une seule requête pour l'accueil public — grille hebdomadaire uniquement.
     */
    @Query("""
            SELECT h FROM Horaire h
            JOIN FETCH h.paroisse p
            LEFT JOIN FETCH p.doyenne
            WHERE h.statusDel = false
              AND h.isActive = true
              AND h.dateSpecifique IS NULL
              AND p.statusDel = false
              AND p.isActive = true
              AND (p.isSystem = false OR p.isSystem IS NULL)
            ORDER BY p.nom ASC, h.heureCelebration ASC
            """)
    List<Horaire> findActiveHorairesForActiveParishes();

    boolean existsByJourSemaineAndHeureCelebrationAndParoisseAndDateSpecifiqueIsNullAndStatusDelFalse(
            JourSemaine jourSemaine,
            LocalTime heureCelebration,
            Paroisse paroisse
    );

    boolean existsByParoisseAndDateSpecifiqueAndHeureCelebrationAndStatusDelFalse(
            Paroisse paroisse,
            LocalDate dateSpecifique,
            LocalTime heureCelebration
    );

    boolean existsByParoisseAndDateSpecifiqueAndUniqueSurParoisseTrueAndStatusDelFalse(
            Paroisse paroisse,
            LocalDate dateSpecifique
    );

    @Query("""
            SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END
            FROM Horaire h
            WHERE h.paroisse = :paroisse
              AND h.dateSpecifique = :dateSpecifique
              AND h.uniqueSurParoisse = true
              AND h.statusDel = false
              AND h.publicId <> :excludePublicId
            """)
    boolean existsOtherUniqueOnDate(
            @Param("paroisse") Paroisse paroisse,
            @Param("dateSpecifique") LocalDate dateSpecifique,
            @Param("excludePublicId") UUID excludePublicId
    );

    @Query("""
            SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END
            FROM Horaire h
            WHERE h.paroisse = :paroisse
              AND h.jourSemaine = :jourSemaine
              AND h.heureCelebration = :heureCelebration
              AND h.dateSpecifique IS NULL
              AND h.statusDel = false
              AND h.publicId <> :excludePublicId
            """)
    boolean existsOtherWeeklySlot(
            @Param("paroisse") Paroisse paroisse,
            @Param("jourSemaine") JourSemaine jourSemaine,
            @Param("heureCelebration") LocalTime heureCelebration,
            @Param("excludePublicId") UUID excludePublicId
    );

    @Query("""
            SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END
            FROM Horaire h
            WHERE h.paroisse = :paroisse
              AND h.dateSpecifique = :dateSpecifique
              AND h.heureCelebration = :heureCelebration
              AND h.statusDel = false
              AND h.publicId <> :excludePublicId
            """)
    boolean existsOtherOneOffSlot(
            @Param("paroisse") Paroisse paroisse,
            @Param("dateSpecifique") LocalDate dateSpecifique,
            @Param("heureCelebration") LocalTime heureCelebration,
            @Param("excludePublicId") UUID excludePublicId
    );

    @EntityGraph(attributePaths = {"paroisse"})
    List<Horaire> findByParoisseAndIsActiveTrueAndStatusDelFalseAndDateSpecifiqueIsNull(Paroisse paroisse);

    @EntityGraph(attributePaths = {"paroisse"})
    List<Horaire> findByParoisseAndIsActiveTrueAndStatusDelFalseAndDateSpecifiqueBetween(
            Paroisse paroisse,
            LocalDate debut,
            LocalDate fin
    );
}
