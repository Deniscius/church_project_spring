package com.eyram.dev.church_project_spring.repository;

import com.eyram.dev.church_project_spring.entities.Facture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FactureRepository extends JpaRepository<Facture, Long> {


    Optional<Facture> findByPublicId(UUID publicId);
    Optional<Facture> findByMontant(Double montant);
    List<Facture> findByDatePaiement(LocalDate datePaiement);
    List<Facture> findByDateProduction(LocalDate dateProduction);
    List<Facture> findByStatusDelFalse();
    List<Facture> findByStatusDelTrue();
    List<Facture> findByStatusDel(Boolean statusDel);
}
