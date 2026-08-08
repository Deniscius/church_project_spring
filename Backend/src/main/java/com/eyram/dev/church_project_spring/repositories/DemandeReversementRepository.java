package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.DemandeReversement;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.enums.StatutReversement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DemandeReversementRepository extends JpaRepository<DemandeReversement, Long> {

    @EntityGraph(attributePaths = {"paroisse"})
    Optional<DemandeReversement> findByPublicIdAndStatusDelFalse(UUID publicId);

    @EntityGraph(attributePaths = {"paroisse"})
    List<DemandeReversement> findByStatusDelFalseOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"paroisse"})
    List<DemandeReversement> findByStatutAndStatusDelFalseOrderByCreatedAtDesc(StatutReversement statut);

    @EntityGraph(attributePaths = {"paroisse"})
    List<DemandeReversement> findByParoisseAndStatusDelFalseOrderByCreatedAtDesc(Paroisse paroisse);
}
