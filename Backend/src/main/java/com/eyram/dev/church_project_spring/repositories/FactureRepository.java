package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.Facture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FactureRepository extends JpaRepository<Facture, Long> {

    Optional<Facture> findByPublicIdAndStatusDelFalse(UUID publicId);

    Optional<Facture> findByRefFactureAndStatusDelFalse(String refFacture);

    Optional<Facture> findByDemandePublicIdAndStatusDelFalse(UUID demandePublicId);

    Optional<Facture> findByDemandeCodeSuivieAndStatusDelFalse(String codeSuivie);

    List<Facture> findAllByStatusDelFalse();

    @Query("""
            SELECT DISTINCT f FROM Facture f
            JOIN FETCH f.demande d
            LEFT JOIN FETCH d.typePaiement
            LEFT JOIN FETCH d.typeDemande
            LEFT JOIN FETCH d.forfaitTarif
            LEFT JOIN FETCH d.paroisse
            WHERE f.statusDel = false
            """)
    List<Facture> findAllActiveWithDemande();

    List<Facture> findByDemande_PublicIdInAndStatusDelFalse(Collection<UUID> demandePublicIds);

    @Query("""
            SELECT DISTINCT f FROM Facture f
            JOIN FETCH f.demande d
            LEFT JOIN FETCH d.typePaiement
            LEFT JOIN FETCH d.typeDemande
            LEFT JOIN FETCH d.forfaitTarif
            LEFT JOIN FETCH d.paroisse
            WHERE f.statusDel = false
              AND d.statusDel = false
              AND d.paroisse.publicId = :paroissePublicId
            ORDER BY f.createdAt DESC
            """)
    List<Facture> findByParoissePublicIdWithDemande(@Param("paroissePublicId") UUID paroissePublicId);
}
