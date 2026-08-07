package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.enums.StatutTenant;
import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "paroisse")
@Getter
@Setter
@NoArgsConstructor
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

    /**
     * Support technique du catalogue plateforme (horaires / types / forfaits
     * par défaut). Ce n'est pas une paroisse cliente : exclue de l'annuaire
     * et des listes métier.
     */
    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = false;

    /**
     * Cycle de vie commercial. {@link #isActive} en est la projection : toute
     * transition doit passer par {@code appliquerStatut} plutôt que de toucher
     * l'un des deux champs isolément, sous peine de violer la contrainte
     * {@code ck_paroisse_actif_suit_statut}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "statut_tenant", length = 30, nullable = false)
    private StatutTenant statutTenant = StatutTenant.PROSPECT;

    @Column(name = "nom_banque", length = 120)
    private String nomBanque;

    @Column(name = "titulaire_compte", length = 150)
    private String titulaireCompte;

    @Column(name = "iban_or_rib", length = 80)
    private String ibanOrRib;

    /** Chemin relatif du logo pour l'en-tête des reçus PDF. */
    @Column(name = "logo_path", length = 500)
    private String logoPath;

    @Column(name = "subscription_expires_at")
    private java.time.LocalDateTime subscriptionExpiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doyenne_id", nullable = false)
    private Doyenne doyenne;

    /**
     * Seul point d'entrée pour changer d'état : positionne le statut et
     * l'interrupteur d'accès ensemble, ce que la base impose de toute façon.
     */
    public void appliquerStatut(StatutTenant statut) {
        this.statutTenant = statut;
        this.isActive = statut == StatutTenant.ACTIVE || statut == StatutTenant.EN_TOLERANCE;
    }

    public Paroisse(Long id, UUID publicId, String nom, String adresse, String email,
                    String telephone, Boolean isActive, Doyenne doyenne) {
        this.id = id;
        this.publicId = publicId;
        this.nom = nom;
        this.adresse = adresse;
        this.email = email;
        this.telephone = telephone;
        this.doyenne = doyenne;
        appliquerStatut(Boolean.TRUE.equals(isActive) ? StatutTenant.ACTIVE : StatutTenant.PROSPECT);
    }
}
