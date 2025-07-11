package com.eyram.dev.church_project_spring.repository;


import com.eyram.dev.church_project_spring.models.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientRepository extends  JpaRepository<Client, Long> {

    Optional<Client> findByNom(String name );
    Optional<Client> findByTel(String tel);

}
