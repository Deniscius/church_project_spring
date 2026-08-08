package com.eyram.dev.church_project_spring.repositories;

import com.eyram.dev.church_project_spring.entities.PasswordResetToken;
import com.eyram.dev.church_project_spring.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHashAndUsedAtIsNull(String tokenHash);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE PasswordResetToken t
            SET t.usedAt = :now
            WHERE t.user = :user AND t.usedAt IS NULL
            """)
    int invalidateUnusedForUser(@Param("user") User user, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :cutoff OR t.usedAt IS NOT NULL")
    int deleteExpiredOrUsed(@Param("cutoff") LocalDateTime cutoff);
}
