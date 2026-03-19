package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.TypeDemande;
import com.eyram.dev.church_project_spring.enums.TypeDemandeEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TypeDemandeRepository extends JpaRepository<TypeDemande, Long> {

    Optional<TypeDemande> findByPublicIdAndStatusDelFalse(UUID publicId);

    List<TypeDemande> findByStatusDelFalse();

    List<TypeDemande> findByParoisseAndStatusDelFalse(Paroisse paroisse);

    List<TypeDemande> findByTypeDemandeEnumAndStatusDelFalse(TypeDemandeEnum typeDemandeEnum);

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