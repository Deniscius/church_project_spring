package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.Doyenne;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.enums.StatutTenant;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParoisseRepository extends JpaRepository<Paroisse, Long> {

    @EntityGraph(attributePaths = {"doyenne"})
    Optional<Paroisse> findByPublicIdAndStatusDelFalse(UUID publicId);

    @Query("""
            SELECT p FROM Paroisse p
            LEFT JOIN FETCH p.doyenne
            WHERE p.publicId = :publicId AND p.statusDel = false
            """)
    Optional<Paroisse> findByPublicIdWithDoyenne(@Param("publicId") UUID publicId);

    @EntityGraph(attributePaths = {"doyenne"})
    List<Paroisse> findAllByStatusDelFalseAndIsActiveTrueAndIsSystemFalseOrderByNomAsc();

    @EntityGraph(attributePaths = {"doyenne"})
    List<Paroisse> findAllByStatusDelFalseAndIsSystemFalseOrderByNomAsc();

    List<Paroisse> findAllByStatutTenantAndDoyenne_PublicIdAndStatusDelFalseAndIsSystemFalseOrderByNomAsc(
            StatutTenant statutTenant,
            UUID doyennePublicId
    );

    @EntityGraph(attributePaths = {"doyenne"})
    Optional<Paroisse> findFirstByIsSystemTrueAndStatusDelFalse();

    boolean existsByNomIgnoreCaseAndDoyenne_PublicIdAndStatusDelFalse(String nom, UUID doyennePublicId);

    /**
     * Retrouve l'entrée d'annuaire correspondant à une demande d'inscription :
     * les 134 paroisses du diocèse préexistent, une inscription les adopte au
     * lieu d'en créer un doublon.
     */
    @EntityGraph(attributePaths = {"doyenne"})
    Optional<Paroisse> findByNomIgnoreCaseAndDoyenne_PublicIdAndStatusDelFalse(
            String nom,
            UUID doyennePublicId
    );

    boolean existsByNomIgnoreCaseAndDoyenne_PublicIdAndStatusDelFalseAndPublicIdNot(
            String nom,
            UUID doyennePublicId,
            UUID publicId
    );

    boolean existsByDoyenneAndStatusDelFalse(Doyenne doyenne);

    boolean existsByEmailIgnoreCaseAndStatusDelFalse(String email);

    boolean existsByEmailIgnoreCaseAndStatusDelFalseAndPublicIdNot(String email, UUID publicId);

    long countByStatusDelFalseAndIsActiveTrue();



    /**
     * Paroisses encore ouvertes alors que leur échéance est dépassée depuis
     * plus longtemps que la tolérance. Une paroisse sans échéance n'a jamais été
     * encaissée : elle est restée en attente de paiement, donc déjà fermée.
     */
    List<Paroisse> findByIsActiveTrueAndStatusDelFalseAndSubscriptionExpiresAtBefore(LocalDateTime limite);
}
