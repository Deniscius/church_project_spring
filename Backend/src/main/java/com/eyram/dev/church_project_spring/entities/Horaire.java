package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.enums.JourSemaine;
import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "horaire")
@Getter
@Setter
public class Horaire extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "jour_semaine", nullable = false)
    private JourSemaine jourSemaine;

    @Column(name = "heure_celebration", nullable = false)
    private LocalTime heureCelebration;

    @Column(name = "libelle", length = 150)
    private String libelle;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paroisse_id", nullable = false)
    private Paroisse paroisse;

    public Horaire(Long id, UUID publicId, JourSemaine jourSemaine, LocalTime heureCelebration, String libelle, Boolean isActive, Paroisse paroisse) {
        this.id = id;
        this.publicId = publicId;
        this.jourSemaine = jourSemaine;
        this.heureCelebration = heureCelebration;
        this.libelle = libelle;
        this.isActive = isActive;
        this.paroisse = paroisse;
    }

    public Horaire() {
        super();

    }
}