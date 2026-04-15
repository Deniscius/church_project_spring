package com.eyram.dev.church_project_spring.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.Check;
import org.hibernate.annotations.UuidGenerator;

import com.eyram.dev.church_project_spring.enums.UserRole;
import com.eyram.dev.church_project_spring.utils.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "username"),
                @UniqueConstraint(columnNames = "public_id")
        }
)
@Getter
@Setter
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "nom", length = 100, nullable = false)
    private String nom;

    @Column(name = "prenom", length = 150, nullable = false)
    private String prenom;

    @Column(name = "username", length = 100, nullable = false, unique = true)
    private String username;

    @JsonIgnore
    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "is_global", nullable = false)
    private Boolean isGlobal = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    @Check(constraints = "(role IN ('SECRETAIRE', 'CURE', 'ADMIN', 'SUPER_ADMIN'))")
    private UserRole role;

    @JsonIgnore
    @OneToMany(mappedBy = "user")
    private List<ParoisseAccess> paroisseAccesses = new ArrayList<>();

    public User() {
    }

    public User(Long id, UUID publicId, String nom, String prenom, String username,
                String password, Boolean isGlobal, Boolean isActive,
                List<ParoisseAccess> paroisseAccesses) {
        this.id = id;
        this.publicId = publicId;
        this.nom = nom;
        this.prenom = prenom;
        this.username = username;
        this.password = password;
        this.isGlobal = isGlobal;
        this.isActive = isActive;
        this.paroisseAccesses = paroisseAccesses;
    }

    @Transient
    public String getFullName() {
        return ((nom != null ? nom : "") + " " + (prenom != null ? prenom : "")).trim();
    }
}