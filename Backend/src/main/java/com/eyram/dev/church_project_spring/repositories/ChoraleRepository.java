package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.Chorale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChoraleRepository extends JpaRepository<Chorale, Long> {
    Optional<Chorale> findByPublicIdAndStatusDelFalse(UUID publicId);
    List<Chorale> findAllByParoisse_PublicIdAndStatusDelFalseOrderByNomAsc(UUID paroissePublicId);
    boolean existsByNomIgnoreCaseAndParoisse_PublicIdAndStatusDelFalse(String nom, UUID paroissePublicId);
    boolean existsByNomIgnoreCaseAndParoisse_PublicIdAndStatusDelFalseAndPublicIdNot(String nom, UUID paroissePublicId, UUID publicId);
}
