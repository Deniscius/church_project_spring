package com.eyram.dev.church_project_spring.entities;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.eyram.dev.church_project_spring.utils.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "paroisse")
@Getter
@Setter
public class Paroisse extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "nom", length = 100, nullable = false)
    private String nom;

    @Column(name = "adresse", length = 200, nullable = false)
    private String adresse;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "telephone", length = 50)
    private String telephone;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "localite_id", nullable = false)
    private Localite localite;

    public Paroisse() {
    }

    public Paroisse(Long id, UUID publicId, String nom, String adresse, String email,
                    String telephone, Boolean isActive, Localite localite) {
        this.id = id;
        this.publicId = publicId;
        this.nom = nom;
        this.adresse = adresse;
        this.email = email;
        this.telephone = telephone;
        this.isActive = isActive;
        this.localite = localite;
    }
}