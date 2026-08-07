package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.ParoisseAbonnement;
import com.eyram.dev.church_project_spring.enums.StatutAbonnement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParoisseAbonnementRepository extends JpaRepository<ParoisseAbonnement, Long> {

    Optional<ParoisseAbonnement> findByPublicIdAndStatusDelFalse(UUID publicId);

    Optional<ParoisseAbonnement> findByIdTransactionAndStatusDelFalse(String idTransaction);

    List<ParoisseAbonnement> findByParoisseAndStatusDelFalseOrderByCreatedAtDesc(Paroisse paroisse);

    Optional<ParoisseAbonnement> findFirstByParoisseAndStatutAndStatusDelFalseOrderByFinAtDesc(
            Paroisse paroisse,
            StatutAbonnement statut
    );

    /** Abonnements actifs dont l'échéance est dépassée : cible du passage en EXPIRE. */
    List<ParoisseAbonnement> findByStatutAndStatusDelFalseAndFinAtBefore(
            StatutAbonnement statut,
            LocalDateTime moment
    );

    @Query("""
            SELECT a FROM ParoisseAbonnement a
            JOIN FETCH a.paroisse p
            LEFT JOIN FETCH p.doyenne
            WHERE a.statusDel = false
            ORDER BY a.createdAt DESC
            """)
    List<ParoisseAbonnement> findAllActiveWithParoisse();
}
