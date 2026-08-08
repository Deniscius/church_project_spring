package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.ParoisseInscription;
import com.eyram.dev.church_project_spring.enums.StatutInscription;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParoisseInscriptionRepository extends JpaRepository<ParoisseInscription, Long> {

    @EntityGraph(attributePaths = {"membres"})
    Optional<ParoisseInscription> findByPublicIdAndStatusDelFalse(UUID publicId);

    @EntityGraph(attributePaths = {"membres"})
    List<ParoisseInscription> findByStatusDelFalseOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"membres"})
    List<ParoisseInscription> findByStatutAndStatusDelFalseOrderByCreatedAtDesc(StatutInscription statut);

    boolean existsByAdminUsernameIgnoreCaseAndStatusDelFalse(String adminUsername);

    /**
     * Empêche deux dossiers concurrents sur la même paroisse d'annuaire : le
     * premier approuvé l'adopterait, le second échouerait à l'approbation.
     */
    boolean existsByNomParoisseIgnoreCaseAndDoyennePublicIdAndStatutAndStatusDelFalse(
            String nomParoisse,
            UUID doyennePublicId,
            StatutInscription statut
    );
}
