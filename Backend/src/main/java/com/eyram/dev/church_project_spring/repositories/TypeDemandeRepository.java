package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.TypeDemande;
import com.eyram.dev.church_project_spring.enums.TypeDemandeEnum;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TypeDemandeRepository extends JpaRepository<TypeDemande, Long> {

    @EntityGraph(attributePaths = {"paroisse", "joursCelebrationAutorises"})
    Optional<TypeDemande> findByPublicIdAndStatusDelFalse(UUID publicId);

    @EntityGraph(attributePaths = {"paroisse", "joursCelebrationAutorises"})
    List<TypeDemande> findByStatusDelFalse();

    @EntityGraph(attributePaths = {"paroisse", "joursCelebrationAutorises"})
    List<TypeDemande> findByParoisseAndStatusDelFalse(Paroisse paroisse);

    @EntityGraph(attributePaths = {"paroisse", "joursCelebrationAutorises"})
    List<TypeDemande> findByTypeDemandeEnumAndStatusDelFalse(TypeDemandeEnum typeDemandeEnum);

    @EntityGraph(attributePaths = {"paroisse", "joursCelebrationAutorises"})
    List<TypeDemande> findByParoisseAndTypeDemandeEnumAndStatusDelFalse(
            Paroisse paroisse,
            TypeDemandeEnum typeDemandeEnum
    );

    boolean existsByLibelleIgnoreCaseAndParoisseAndTypeDemandeEnumAndStatusDelFalse(
            String libelle,
            Paroisse paroisse,
            TypeDemandeEnum typeDemandeEnum
    );
}
