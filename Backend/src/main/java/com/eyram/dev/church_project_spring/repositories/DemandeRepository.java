package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.entities.Fidele;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DemandeRepository extends JpaRepository<Demande, Long> {

    // Déjà fourni par JpaRepository : Optional<Demande> findById(Long id);

    Optional<Demande> findByPublicId(UUID publicId);
    Optional<Demande> findByPublicIdAndStatusDelFalse(UUID publicId);
    List<Demande> findByStatusDel(Boolean statusDel);          // ou Page<Demande> ...
    List<Demande> findByStatusDelFalse();
    Page<Demande> findByStatusDelFalse(Pageable pageable);
    List<Demande> findByDateDemande(LocalDate dateDemande);
    List<Demande> findByFidele(Fidele fidele);
    List<Demande> findByFideleAndStatusDelFalse(Fidele fidele);
    Page<Demande> findByFideleAndStatusDelFalse(Fidele fidele, Pageable pageable);
    List<Demande> findByDateDemandeBetween(LocalDate start, LocalDate end);
    List<Demande> findByFideleAndDateDemandeBetweenAndStatusDelFalse(Fidele f, LocalDate start, LocalDate end);
}
