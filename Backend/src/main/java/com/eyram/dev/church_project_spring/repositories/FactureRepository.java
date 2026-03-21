package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.DetailsPaiement;
import com.eyram.dev.church_project_spring.entities.Facture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FactureRepository extends JpaRepository<Facture, Long> {

    Optional<Facture> findByPublicIdAndStatusDelFalse(UUID publicId);

    Optional<Facture> findByRefFactureAndStatusDelFalse(String refFacture);

    Optional<Facture> findByDemandePublicIdAndStatusDelFalse(UUID demandePublicId);

    Optional<Facture> findByDemandeCodeSuivieAndStatusDelFalse(String codeSuivie);

    Optional<DetailsPaiement> findByFacturePublicIdAndStatusDelFalse(UUID facturePublicId);



    List<Facture> findAllByStatusDelFalse();
}