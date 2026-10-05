package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.StructurePastorale;
import com.eyram.dev.church_project_spring.enums.TypeStructurePastorale;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StructurePastoraleRepository extends JpaRepository<StructurePastorale, Long> {

    @EntityGraph(attributePaths = "paroisse")
    Optional<StructurePastorale> findByPublicIdAndStatusDelFalse(UUID publicId);

    @EntityGraph(attributePaths = "paroisse")
    List<StructurePastorale> findByParoisseAndStatusDelFalseOrderByTypeAscOrdreAffichageAscNomAsc(
            Paroisse paroisse
    );

    boolean existsByParoisseAndTypeAndNomIgnoreCaseAndStatusDelFalse(
            Paroisse paroisse,
            TypeStructurePastorale type,
            String nom
    );

    boolean existsByParoisseAndTypeAndNomIgnoreCaseAndStatusDelFalseAndPublicIdNot(
            Paroisse paroisse,
            TypeStructurePastorale type,
            String nom,
            UUID publicId
    );
}
