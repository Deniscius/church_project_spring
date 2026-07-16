package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.User;
import com.eyram.dev.church_project_spring.enums.UserRole;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPublicIdAndStatusDelFalse(UUID publicId);

    @EntityGraph(attributePaths = {
            "paroisseAccesses",
            "paroisseAccesses.paroisse"
    })
    Optional<User> findByUsernameIgnoreCaseAndStatusDelFalse(String username);

    boolean existsByUsernameIgnoreCaseAndStatusDelFalse(String username);

    List<User> findByStatusDelFalseOrderByNomAscPrenomAsc();

    long countByStatusDelFalse();

    long countByStatusDelFalseAndIsActiveTrueAndIsGlobalTrueAndRole(UserRole role);
}
