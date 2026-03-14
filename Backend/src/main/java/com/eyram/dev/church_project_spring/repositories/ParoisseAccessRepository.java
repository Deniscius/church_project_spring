package com.eyram.dev.church_project_spring.repository;

import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.ParoisseAccess;
import com.eyram.dev.church_project_spring.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParoisseAccessRepository extends JpaRepository<ParoisseAccess, Long> {

    Optional<ParoisseAccess> findByPublicIdAndStatusDelFalse(UUID publicId);

    List<ParoisseAccess> findByStatusDelFalse();

    List<ParoisseAccess> findByUserAndStatusDelFalse(User user);

    List<ParoisseAccess> findByParoisseAndStatusDelFalse(Paroisse paroisse);

    List<ParoisseAccess> findByUserAndActiveTrueAndStatusDelFalse(User user);

    Optional<ParoisseAccess> findByUserAndParoisseAndStatusDelFalse(User user, Paroisse paroisse);

    boolean existsByUserAndParoisseAndStatusDelFalse(User user, Paroisse paroisse);

    boolean existsByUserAndParoisseAndActiveTrueAndStatusDelFalse(User user, Paroisse paroisse);
}