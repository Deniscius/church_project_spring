package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPublicIdAndStatusDelFalse(UUID publicId);

    Optional<User> findByUsernameAndStatusDelFalse(String username);

    boolean existsByUsernameAndStatusDelFalse(String username);

    List<User> findByStatusDelFalse();

    long countByStatusDelFalse();
}