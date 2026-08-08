package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.CompteParoisse;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CompteParoisseRepository extends JpaRepository<CompteParoisse, Long> {

    Optional<CompteParoisse> findByParoisseAndStatusDelFalse(Paroisse paroisse);

    Optional<CompteParoisse> findByParoisse_PublicIdAndStatusDelFalse(UUID paroissePublicId);

    Optional<CompteParoisse> findByPublicIdAndStatusDelFalse(UUID publicId);
}
