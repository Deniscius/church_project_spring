package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.enums.RoleParoisse;
import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(
        name = "paroisse_access",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "paroisse_id"})
        }
)
@Getter
@Setter
public class ParoisseAccess extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paroisse_id", nullable = false)
    private Paroisse paroisse;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_paroisse", nullable = false, length = 50)
    private RoleParoisse roleParoisse;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    public ParoisseAccess() {
    }

    public ParoisseAccess(Long id, UUID publicId, User user, Paroisse paroisse,
                          RoleParoisse roleParoisse, Boolean active) {
        this.id = id;
        this.publicId = publicId;
        this.user = user;
        this.paroisse = paroisse;
        this.roleParoisse = roleParoisse;
        this.active = active;
    }
}