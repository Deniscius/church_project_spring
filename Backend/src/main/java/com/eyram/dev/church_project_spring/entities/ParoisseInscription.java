package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.enums.PlanAbonnement;
import com.eyram.dev.church_project_spring.enums.StatutInscription;
import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "paroisse_inscription")
@Getter
@Setter
public class ParoisseInscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "nom_paroisse", nullable = false, length = 100)
    private String nomParoisse;

    @Column(name = "adresse", nullable = false, length = 200)
    private String adresse;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "telephone", length = 50)
    private String telephone;

    @Column(name = "doyenne_public_id", nullable = false)
    private UUID doyennePublicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_abonnement", nullable = false, length = 30)
    private PlanAbonnement planAbonnement;

    @Column(name = "admin_nom", nullable = false, length = 80)
    private String adminNom;

    @Column(name = "admin_prenom", nullable = false, length = 80)
    private String adminPrenom;

    @Column(name = "admin_email", length = 150)
    private String adminEmail;

    @Column(name = "admin_telephone", length = 50)
    private String adminTelephone;

    @Column(name = "admin_username", nullable = false, length = 80)
    private String adminUsername;

    /** Null après approbation / rejet (secret copié sur User puis purgé). */
    @Column(name = "admin_password_hash", length = 255)
    private String adminPasswordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    private StatutInscription statut = StatutInscription.SOUMISE;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "paroisse_public_id")
    private UUID paroissePublicId;

    /** Scan du mandat du curé autorisant l'inscription sur la plateforme. */
    @Column(name = "mandat_cure_path", length = 500)
    private String mandatCurePath;

    /** Scan de la pièce d'identité du premier administrateur. */
    @Column(name = "admin_cni_path", length = 500)
    private String adminCniPath;

    @OneToMany(mappedBy = "inscription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ParoisseInscriptionMembre> membres = new ArrayList<>();
}
