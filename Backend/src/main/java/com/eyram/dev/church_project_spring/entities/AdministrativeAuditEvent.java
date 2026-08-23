package com.eyram.dev.church_project_spring.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Trace append-only des décisions administratives sensibles.
 *
 * Aucun secret, document ou mot de passe ne doit être enregistré dans details.
 */
@Entity
@Table(name = "administrative_audit_event")
@Getter
@Setter
public class AdministrativeAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "action", nullable = false, length = 80, updatable = false)
    private String action;

    @Column(name = "target_type", nullable = false, length = 60, updatable = false)
    private String targetType;

    @Column(name = "target_public_id", nullable = false, updatable = false)
    private UUID targetPublicId;

    @Column(name = "paroisse_public_id", updatable = false)
    private UUID paroissePublicId;

    @Column(name = "actor_public_id", updatable = false)
    private UUID actorPublicId;

    @Column(name = "actor_username", nullable = false, length = 100, updatable = false)
    private String actorUsername;

    @Column(name = "actor_name", nullable = false, length = 160, updatable = false)
    private String actorName;

    @Column(name = "details", length = 500, updatable = false)
    private String details;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    void assignOccurredAt() {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }
}
