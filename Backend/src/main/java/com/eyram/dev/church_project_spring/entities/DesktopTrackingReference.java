package com.eyram.dev.church_project_spring.entities;

import jakarta.persistence.*;
import java.util.UUID;

/** Read model used by public tracking lookups; inserted with the desktop operation transaction. */
@Entity
@Table(name="desktop_tracking_reference")
public class DesktopTrackingReference {
    @Id @Column(name="reference",length=80) private String reference;
    @Column(name="demande_public_id",nullable=false) private UUID demandePublicId;
    @Column(name="owner_id",nullable=false) private UUID ownerId;
    @Column(name="operation_id",nullable=false) private UUID operationId;
    protected DesktopTrackingReference(){}
}
