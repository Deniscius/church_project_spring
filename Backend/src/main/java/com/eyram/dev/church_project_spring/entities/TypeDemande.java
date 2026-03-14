package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "type_demande")
@Getter
@Setter
public class TypeDemande extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "libelle", nullable = false, length = 150)
    private String libelle;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paroisse_id", nullable = false)
    private Paroisse paroisse;

    public TypeDemande() {
    }

    public TypeDemande(Long id, UUID publicId, String libelle, String description, Boolean isActive, Paroisse paroisse) {
        this.id = id;
        this.publicId = publicId;
        this.libelle = libelle;
        this.description = description;
        this.isActive = isActive;
        this.paroisse = paroisse;
    }
}