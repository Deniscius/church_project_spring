package com.eyram.dev.church_project_spring.entities;


import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "forfai_tarif")
@Getter
@Setter
public class ForfaitTarif extends BaseEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "nom_forfait")
    private String nom_forfait;

    @Column(name = "nombre_jours")
    private int nombreJours;

    @Column(name = "tarif")
    private double tarif;


    @Column(name = "description")
    private String description;




}
