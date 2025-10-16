package com.eyram.dev.church_project_spring.repository;

import com.eyram.dev.church_project_spring.entities.Fidele;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FideleRepository extends JpaRepository<Fidele, Long> {

    Optional<Fidele> findByNom(String name);
    Optional<Fidele> findByTel(String tel);
    Optional<Fidele> findByPublicId(UUID publicId);
    List<Fidele> findAllByNom(String nom);

    List<Fidele> findByStatusDelTrue();
    List<Fidele> findByStatusDelFalse();
}
