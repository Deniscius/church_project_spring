package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.TypePaiement;
import com.eyram.dev.church_project_spring.enums.StatutDemandeEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DemandeRepository extends JpaRepository<Demande, Long> {

    Optional<Demande> findByPublicIdAndStatusDelFalse(UUID publicId);

    Optional<Demande> findByCodeSuivieAndStatusDelFalse(String codeSuivie);

    @Query("""
            SELECT d.codeSuivie FROM Demande d
            WHERE d.statusDel = false
              AND d.telFidele IN :phones
            ORDER BY d.createdAt DESC
            """)
    List<String> findCodesByTelFideleIn(@Param("phones") Collection<String> phones, Pageable pageable);

    @Query("""
            SELECT d FROM Demande d
            WHERE d.statusDel = false
              AND d.telFidele IN :phones
            ORDER BY d.createdAt DESC
            """)
    List<Demande> findByTelFideleIn(@Param("phones") Collection<String> phones, Pageable pageable);

    List<Demande> findByStatusDelFalse();

    List<Demande> findByParoisseAndStatusDelFalse(Paroisse paroisse);

    @EntityGraph(attributePaths = {
            "paroisse", "typeDemande", "forfaitTarif", "horaire", "user", "typePaiement"
    })
    Page<Demande> findByParoisseAndStatusDelFalse(Paroisse paroisse, Pageable pageable);

    /** Liste complète (actives + soft-supprimées) — audit comptable. */
    @EntityGraph(attributePaths = {
            "paroisse", "typeDemande", "forfaitTarif", "horaire", "user", "typePaiement"
    })
    Page<Demande> findByParoisse(Paroisse paroisse, Pageable pageable);

    /**
     * Recherche paginée dans une paroisse. Le téléphone est stocké en E.164 ;
     * {@code phonePattern} reçoit donc uniquement les chiffres saisis afin
     * d'accepter « 90 12 34 56 », « +22890123456 » ou « 90123456 ».
     */
    @EntityGraph(attributePaths = {
            "paroisse", "typeDemande", "forfaitTarif", "horaire", "user", "typePaiement"
    })
    @Query("""
            SELECT d FROM Demande d
            WHERE d.paroisse = :paroisse
              AND (:includeDeleted = true OR d.statusDel = false)
              AND (
                    LOWER(d.codeSuivie) LIKE :textPattern
                 OR LOWER(d.prenomFidele) LIKE :textPattern
                 OR LOWER(d.nomFidele) LIKE :textPattern
                 OR LOWER(CONCAT(d.prenomFidele, CONCAT(' ', d.nomFidele))) LIKE :textPattern
                 OR LOWER(CONCAT(d.nomFidele, CONCAT(' ', d.prenomFidele))) LIKE :textPattern
                 OR d.telFidele LIKE :phonePattern
              )
            """)
    Page<Demande> searchByParoisse(
            @Param("paroisse") Paroisse paroisse,
            @Param("includeDeleted") boolean includeDeleted,
            @Param("textPattern") String textPattern,
            @Param("phonePattern") String phonePattern,
            Pageable pageable
    );

    List<Demande> findByTypePaiementPublicIdAndStatusDelFalse(UUID typePaiementPublicId);

    List<Demande> findByParoisseAndStatutDemandeAndStatusDelFalse(Paroisse paroisse, StatutDemandeEnum statutDemande);

    boolean existsByCodeSuivieAndStatusDelFalse(String codeSuivie);

    boolean existsByTypePaiementAndStatusDelFalse(TypePaiement typePaiement);

    long countByParoisseAndStatusDelFalse(Paroisse paroisse);

    long countByParoisseAndStatutDemandeAndStatusDelFalse(Paroisse paroisse, StatutDemandeEnum statutDemande);

    @Query("""
            SELECT COALESCE(SUM(d.montant), 0)
            FROM Demande d
            WHERE d.paroisse = :paroisse AND d.statusDel = false
            """)
    BigDecimal sumMontantByParoisse(@Param("paroisse") Paroisse paroisse);

    @Query("""
            SELECT DISTINCT d FROM Demande d
            LEFT JOIN FETCH d.paroisse
            LEFT JOIN FETCH d.typeDemande
            LEFT JOIN FETCH d.forfaitTarif
            LEFT JOIN FETCH d.horaire
            LEFT JOIN FETCH d.user
            LEFT JOIN FETCH d.typePaiement
            WHERE d.statusDel = false AND d.paroisse = :paroisse
            """)
    List<Demande> findByParoisseWithAssociations(@Param("paroisse") Paroisse paroisse);

    @Query("""
            SELECT DISTINCT d FROM Demande d
            LEFT JOIN FETCH d.paroisse
            LEFT JOIN FETCH d.typeDemande
            LEFT JOIN FETCH d.forfaitTarif
            LEFT JOIN FETCH d.horaire
            LEFT JOIN FETCH d.user
            LEFT JOIN FETCH d.typePaiement
            WHERE d.statusDel = false
              AND d.paroisse = :paroisse
              AND d.statutDemande = :statutDemande
            """)
    List<Demande> findByParoisseAndStatutWithAssociations(
            @Param("paroisse") Paroisse paroisse,
            @Param("statutDemande") StatutDemandeEnum statutDemande
    );

    @Query("""
            SELECT DISTINCT d FROM Demande d
            LEFT JOIN FETCH d.paroisse
            LEFT JOIN FETCH d.typeDemande
            LEFT JOIN FETCH d.forfaitTarif
            LEFT JOIN FETCH d.horaire
            LEFT JOIN FETCH d.user
            LEFT JOIN FETCH d.typePaiement
            WHERE d.statusDel = false
            """)
    List<Demande> findAllActiveWithAssociations();

    /** Audit plateforme : toutes les demandes (actives + soft-supprimées). */
    @EntityGraph(attributePaths = {
            "paroisse", "typeDemande", "forfaitTarif", "horaire", "user", "typePaiement"
    })
    @Query("SELECT d FROM Demande d")
    Page<Demande> findAllForPlatformAudit(Pageable pageable);

    @EntityGraph(attributePaths = {
            "paroisse", "typeDemande", "forfaitTarif", "horaire", "user", "typePaiement"
    })
    @Query("SELECT d FROM Demande d WHERE d.statusDel = false")
    Page<Demande> findActiveForPlatformAudit(Pageable pageable);

    @Query("""
            SELECT DISTINCT d FROM Demande d
            LEFT JOIN FETCH d.paroisse
            LEFT JOIN FETCH d.typeDemande
            LEFT JOIN FETCH d.forfaitTarif
            LEFT JOIN FETCH d.horaire
            LEFT JOIN FETCH d.user
            LEFT JOIN FETCH d.typePaiement
            WHERE d.statusDel = false AND d.publicId = :publicId
            """)
    Optional<Demande> findByPublicIdWithAssociations(@Param("publicId") UUID publicId);

    @Query("""
            SELECT DISTINCT d FROM Demande d
            LEFT JOIN FETCH d.paroisse p
            LEFT JOIN FETCH p.doyenne
            LEFT JOIN FETCH d.typeDemande
            LEFT JOIN FETCH d.forfaitTarif
            LEFT JOIN FETCH d.horaire
            LEFT JOIN FETCH d.user
            LEFT JOIN FETCH d.typePaiement
            WHERE d.statusDel = false AND d.codeSuivie = :codeSuivie
            """)
    Optional<Demande> findByCodeSuivieWithAssociations(@Param("codeSuivie") String codeSuivie);

    @Query("""
            SELECT DISTINCT d FROM Demande d
            LEFT JOIN FETCH d.paroisse
            LEFT JOIN FETCH d.typeDemande
            LEFT JOIN FETCH d.forfaitTarif
            LEFT JOIN FETCH d.horaire
            LEFT JOIN FETCH d.user
            LEFT JOIN FETCH d.typePaiement
            WHERE d.statusDel = false AND d.typePaiement.publicId = :typePaiementPublicId
            """)
    List<Demande> findByTypePaiementWithAssociations(@Param("typePaiementPublicId") UUID typePaiementPublicId);

    @Query("""
            SELECT DISTINCT d FROM Demande d
            LEFT JOIN FETCH d.paroisse
            LEFT JOIN FETCH d.typeDemande
            LEFT JOIN FETCH d.forfaitTarif
            LEFT JOIN FETCH d.horaire
            LEFT JOIN FETCH d.user
            LEFT JOIN FETCH d.typePaiement
            WHERE d.statusDel = true AND d.paroisse = :paroisse
            ORDER BY d.deletedAt DESC
            """)
    List<Demande> findDeletedByParoisseWithAssociations(@Param("paroisse") Paroisse paroisse);

    @EntityGraph(attributePaths = {
            "paroisse", "typeDemande", "forfaitTarif", "horaire", "user", "typePaiement"
    })
    Page<Demande> findByParoisseAndStatusDelTrue(Paroisse paroisse, Pageable pageable);

    @Query("""
            SELECT DISTINCT d FROM Demande d
            LEFT JOIN FETCH d.paroisse
            LEFT JOIN FETCH d.typeDemande
            LEFT JOIN FETCH d.forfaitTarif
            LEFT JOIN FETCH d.horaire
            LEFT JOIN FETCH d.user
            LEFT JOIN FETCH d.typePaiement
            WHERE d.publicId = :publicId
            """)
    Optional<Demande> findByPublicIdWithAssociationsIncludingDeleted(@Param("publicId") UUID publicId);
}
