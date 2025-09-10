package com.eyram.dev.church_project_spring.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "type_paiement")
@Data
public class TypePaiement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id")
    private UUID publicId;

    @Column(name = "libelle")
    private String libelle;

    @Column(name = "status_del")
    private Boolean statusDel = false;

    public TypePaiement() {

    }
}
