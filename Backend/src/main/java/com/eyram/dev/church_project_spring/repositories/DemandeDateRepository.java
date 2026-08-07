package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.entities.DemandeDate;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.enums.StatutDemandeEnum;
import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DemandeDateRepository extends JpaRepository<DemandeDate, Long> {

    List<DemandeDate> findByDemandeAndStatusDelFalseOrderByOrdreAsc(Demande demande);

    Optional<DemandeDate> findByPublicIdAndStatusDelFalse(UUID publicId);

    List<DemandeDate> findByStatusDelFalse();

    List<DemandeDate> findByDemandePublicIdAndStatusDelFalse(UUID demandePublicId);

    List<DemandeDate> findByDemande_Id(Long demandeId);

    List<DemandeDate> findByDemande_IdAndStatusDelFalse(Long demandeId);

    boolean existsByDemandeAndOrdreAndStatusDelFalse(Demande demande, Integer ordre);

    boolean existsByDemandeAndDateCelebrationAndStatusDelFalse(Demande demande, LocalDate dateCelebration);

    List<DemandeDate> findByDemande_IdInAndStatusDelFalseOrderByOrdreAsc(Collection<Long> demandeIds);

    @Query("""
            SELECT dd FROM DemandeDate dd
            JOIN FETCH dd.demande d
            JOIN FETCH d.paroisse p
            JOIN FETCH d.typeDemande
            LEFT JOIN FETCH d.forfaitTarif
            LEFT JOIN FETCH d.horaire
            LEFT JOIN FETCH dd.horaire
            WHERE dd.statusDel = false
              AND d.statusDel = false
              AND p.publicId = :paroissePublicId
              AND dd.dateCelebration = :dateCelebration
              AND d.statutDemande IN :statutsDemande
              AND (:inclureNonPayees = true OR d.statutPaiement = :statutPaye)
            """)
    List<DemandeDate> findForFeuilleCelebration(
            @Param("paroissePublicId") UUID paroissePublicId,
            @Param("dateCelebration") LocalDate dateCelebration,
            @Param("statutsDemande") Collection<StatutDemandeEnum> statutsDemande,
            @Param("inclureNonPayees") boolean inclureNonPayees,
            @Param("statutPaye") StatutPaiementEnum statutPaye
    );

    /**
     * Premières célébrations (ordre = 1) de demandes actives non payées,
     * candidates à l'annulation automatique avant la messe.
     */
    @Query("""
            SELECT dd FROM DemandeDate dd
            JOIN FETCH dd.demande d
            LEFT JOIN FETCH d.horaire
            WHERE dd.statusDel = false
              AND d.statusDel = false
              AND dd.ordre = 1
              AND dd.dateCelebration <= :maxDate
              AND d.statutPaiement <> :statutPaye
              AND d.statutDemande IN :statutsDemande
            """)
    List<DemandeDate> findFirstCelebrationsUnpaidForCancel(
            @Param("maxDate") LocalDate maxDate,
            @Param("statutPaye") StatutPaiementEnum statutPaye,
            @Param("statutsDemande") Collection<StatutDemandeEnum> statutsDemande
    );

    /**
     * Premières célébrations de demandes non payées dans la fenêtre de rappel (D-N).
     */
    @Query("""
            SELECT dd FROM DemandeDate dd
            JOIN FETCH dd.demande d
            LEFT JOIN FETCH d.horaire
            LEFT JOIN FETCH d.paroisse
            LEFT JOIN FETCH d.typeDemande
            LEFT JOIN FETCH d.forfaitTarif
            WHERE dd.statusDel = false
              AND d.statusDel = false
              AND dd.ordre = 1
              AND dd.dateCelebration >= :minDate
              AND dd.dateCelebration <= :maxDate
              AND d.statutPaiement <> :statutPaye
              AND d.statutDemande IN :statutsDemande
            """)
    List<DemandeDate> findFirstCelebrationsUnpaidForReminder(
            @Param("minDate") LocalDate minDate,
            @Param("maxDate") LocalDate maxDate,
            @Param("statutPaye") StatutPaiementEnum statutPaye,
            @Param("statutsDemande") Collection<StatutDemandeEnum> statutsDemande
    );

    /**
     * Même fenêtre, limitée à une paroisse (alerte dashboard admin local).
     */
    @Query("""
            SELECT dd FROM DemandeDate dd
            JOIN FETCH dd.demande d
            LEFT JOIN FETCH d.horaire
            LEFT JOIN FETCH d.typeDemande
            LEFT JOIN FETCH d.forfaitTarif
            WHERE dd.statusDel = false
              AND d.statusDel = false
              AND d.paroisse = :paroisse
              AND dd.ordre = 1
              AND dd.dateCelebration >= :minDate
              AND dd.dateCelebration <= :maxDate
              AND d.statutPaiement <> :statutPaye
              AND d.statutDemande IN :statutsDemande
            ORDER BY dd.dateCelebration ASC
            """)
    List<DemandeDate> findFirstCelebrationsUnpaidForReminderByParoisse(
            @Param("paroisse") Paroisse paroisse,
            @Param("minDate") LocalDate minDate,
            @Param("maxDate") LocalDate maxDate,
            @Param("statutPaye") StatutPaiementEnum statutPaye,
            @Param("statutsDemande") Collection<StatutDemandeEnum> statutsDemande
    );
}
