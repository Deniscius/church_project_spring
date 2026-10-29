package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.ChoraleInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface ChoraleInvitationRepository extends JpaRepository<ChoraleInvitation, Long> {
    Optional<ChoraleInvitation> findByTokenHashAndStatusDelFalse(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ChoraleInvitation i where i.tokenHash = :hash and i.statusDel = false")
    Optional<ChoraleInvitation> findForUpdateByTokenHash(@Param("hash") String hash);
}
