package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.ParoisseInscription;
import com.eyram.dev.church_project_spring.enums.StatutInscription;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParoisseInscriptionRepository extends JpaRepository<ParoisseInscription, Long> {

    @EntityGraph(attributePaths = {"membres"})
    Optional<ParoisseInscription> findByPublicIdAndStatusDelFalse(UUID publicId);

    /**
     * Sérialise les décisions administratives sur un même dossier.
     *
     * La collection membres reste volontairement chargée paresseusement dans
     * la transaction : un graphe avec collection et FOR UPDATE peut produire
     * un verrouillage invalide sur le côté nullable d'une jointure PostgreSQL.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i FROM ParoisseInscription i
            WHERE i.publicId = :publicId
              AND i.statusDel = false
            """)
    Optional<ParoisseInscription> findByPublicIdForUpdate(@Param("publicId") UUID publicId);

    @EntityGraph(attributePaths = {"membres"})
    List<ParoisseInscription> findByStatusDelFalseOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"membres"})
    List<ParoisseInscription> findByStatutAndStatusDelFalseOrderByCreatedAtDesc(StatutInscription statut);

    boolean existsByAdminUsernameIgnoreCaseAndStatusDelFalse(String adminUsername);

    /**
     * Empêche deux dossiers concurrents sur la même paroisse d'annuaire : le
     * premier approuvé l'adopterait, le second échouerait à l'approbation.
     */
    boolean existsByNomParoisseIgnoreCaseAndDoyennePublicIdAndStatutAndStatusDelFalse(
            String nomParoisse,
            UUID doyennePublicId,
            StatutInscription statut
    );
}
