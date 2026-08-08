package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.enums.TypeEcriture;
import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "ecriture_comptable")
@Getter
@Setter
public class EcritureComptable extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paroisse_id", nullable = false)
    private Paroisse paroisse;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_ecriture", nullable = false, length = 40)
    private TypeEcriture typeEcriture;

    @Column(name = "montant", nullable = false)
    private Integer montant;

    @Column(name = "libelle", nullable = false, length = 255)
    private String libelle;

    @Column(name = "reference_externe", length = 150)
    private String referenceExterne;

    @Column(name = "demande_code_suivie", length = 80)
    private String demandeCodeSuivie;
}
