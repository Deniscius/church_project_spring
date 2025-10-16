package com.eyram.dev.church_project_spring.repository;

import com.eyram.dev.church_project_spring.entities.Fidele;
import com.eyram.dev.church_project_spring.entities.Demande;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface DemandeRepository extends JpaRepository<Demande, Long> {
    Optional<Demande> findById(Long id);
    Optional<Demande> findByPublicId(UUID publicId);
    Optional<Demande> findByStatusDel(Boolean statusDel);
    Optional<Demande> findByDateDemande(Date dateDemande);
    List<Demande> findByFidele(Fidele fidele);


}

