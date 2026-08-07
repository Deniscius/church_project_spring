package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.DetailsPaiement;
import com.eyram.dev.church_project_spring.entities.TypePaiement;
import com.eyram.dev.church_project_spring.enums.ModePaiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DetailsPaiementRepository extends JpaRepository<DetailsPaiement, Long> {

    Optional<DetailsPaiement> findByPublicIdAndStatusDelFalse(UUID publicId);

    List<DetailsPaiement> findAllByStatusDelFalse();

    @Query("""
            SELECT DISTINCT dp FROM DetailsPaiement dp
            JOIN FETCH dp.facture f
            JOIN FETCH dp.typePaiement
            LEFT JOIN FETCH f.demande d
            LEFT JOIN FETCH d.typePaiement
            LEFT JOIN FETCH d.paroisse
            WHERE dp.statusDel = false
            """)
    List<DetailsPaiement> findAllActiveWithAssociations();

    Optional<DetailsPaiement> findByFacturePublicId(UUID facturePublicId);

    Optional<DetailsPaiement> findByFacturePublicIdAndStatusDelFalse(UUID facturePublicId);

    Optional<DetailsPaiement> findByIdTransactionAndStatusDelFalse(String idTransaction);

    boolean existsByTypePaiementAndStatusDelFalse(TypePaiement typePaiement);

    boolean existsByIdTransactionAndStatusDelFalse(String idTransaction);

    List<DetailsPaiement> findByFacture_PublicIdInAndStatusDelFalse(Collection<UUID> facturePublicIds);

    @Query("""
            SELECT DISTINCT dp FROM DetailsPaiement dp
            JOIN FETCH dp.facture f
            JOIN FETCH dp.typePaiement tp
            JOIN FETCH f.demande d
            JOIN FETCH d.paroisse p
            WHERE dp.statusDel = false
              AND dp.statutPaiement = com.eyram.dev.church_project_spring.enums.StatutPaiementEnum.PAYE
              AND tp.mode = :mode
              AND p.publicId = :paroissePublicId
            ORDER BY dp.dateDetailsPaiement DESC
            """)
    List<DetailsPaiement> findCaisseLocaleByParoisse(
            @Param("paroissePublicId") UUID paroissePublicId,
            @Param("mode") ModePaiement mode
    );
}
