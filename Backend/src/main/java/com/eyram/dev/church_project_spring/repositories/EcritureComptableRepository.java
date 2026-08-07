package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.EcritureComptable;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EcritureComptableRepository extends JpaRepository<EcritureComptable, Long> {

    List<EcritureComptable> findByParoisseAndStatusDelFalseOrderByCreatedAtDesc(Paroisse paroisse);

    boolean existsByReferenceExterneAndStatusDelFalse(String referenceExterne);
}
