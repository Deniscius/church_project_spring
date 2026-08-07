package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.ParoisseAccess;
import com.eyram.dev.church_project_spring.entities.User;
import com.eyram.dev.church_project_spring.enums.RoleParoisse;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParoisseAccessRepository extends JpaRepository<ParoisseAccess, Long> {

    // Le doyenné fait partie de la fiche d'accès restituée à la plateforme :
    // le charger ici évite une requête par ligne.
    @EntityGraph(attributePaths = {"user", "paroisse", "paroisse.doyenne"})
    Optional<ParoisseAccess> findByPublicIdAndStatusDelFalse(UUID publicId);

    @EntityGraph(attributePaths = {"user", "paroisse", "paroisse.doyenne"})
    List<ParoisseAccess> findByStatusDelFalse();

    @EntityGraph(attributePaths = {"user", "paroisse", "paroisse.doyenne"})
    List<ParoisseAccess> findByUserAndStatusDelFalse(User user);

    @EntityGraph(attributePaths = {"user", "paroisse", "paroisse.doyenne"})
    List<ParoisseAccess> findByParoisseAndStatusDelFalse(Paroisse paroisse);

    @EntityGraph(attributePaths = {"user", "paroisse", "paroisse.doyenne"})
    List<ParoisseAccess> findByUserAndActiveTrueAndStatusDelFalse(User user);

    Optional<ParoisseAccess> findByUserAndParoisseAndStatusDelFalse(User user, Paroisse paroisse);

    boolean existsByUserAndParoisseAndStatusDelFalse(User user, Paroisse paroisse);

    boolean existsByUserAndParoisseAndActiveTrueAndStatusDelFalse(User user, Paroisse paroisse);

    @Query("""
            SELECT COUNT(a) FROM ParoisseAccess a
            WHERE a.paroisse = :paroisse
              AND a.roleParoisse = :role
              AND a.active = true
              AND a.statusDel = false
              AND a.user.statusDel = false
              AND a.user.isActive = true
              AND a.user.publicId <> :excludeUserId
            """)
    long countOtherActiveAdmins(
            @Param("paroisse") Paroisse paroisse,
            @Param("role") RoleParoisse role,
            @Param("excludeUserId") UUID excludeUserId
    );
}
