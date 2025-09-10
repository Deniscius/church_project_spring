package com.eyram.dev.church_project_spring.repository;

import com.eyram.dev.church_project_spring.entities.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByNom(String name);
    Optional<Client> findByTel(String tel);
    Optional<Client> findByPublicId(UUID publicId);
    List<Client> findAllByNom(String nom);

    List<Client> findByStatusDelTrue();
    List<Client> findByStatusDelFalse();
}
