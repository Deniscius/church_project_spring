package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.CompteParoisse;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CompteParoisseRepository extends JpaRepository<CompteParoisse, Long> {

    Optional<CompteParoisse> findByParoisseAndStatusDelFalse(Paroisse paroisse);

    /**
     * Verrouille le compte jusqu'à la fin de la transaction afin de sérialiser
     * les crédits, réservations et confirmations concurrents.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c FROM CompteParoisse c
            WHERE c.paroisse = :paroisse
              AND c.statusDel = false
            """)
    Optional<CompteParoisse> findByParoisseForUpdate(@Param("paroisse") Paroisse paroisse);

    Optional<CompteParoisse> findByParoisse_PublicIdAndStatusDelFalse(UUID paroissePublicId);

    Optional<CompteParoisse> findByPublicIdAndStatusDelFalse(UUID publicId);
}
