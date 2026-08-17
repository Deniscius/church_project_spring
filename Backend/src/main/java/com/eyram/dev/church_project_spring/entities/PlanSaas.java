package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.enums.PlanAbonnement;
import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/**
 * Catalogue tarifaire global du SaaS.
 *
 * Le code du plan reste porté par {@link PlanAbonnement} pour préserver la
 * compatibilité avec les inscriptions et abonnements existants. Les valeurs
 * commerciales (prix, durée, libellé) sont administrables sans redéploiement.
 */
@Entity
@Table(
        name = "plan_saas",
        uniqueConstraints = @UniqueConstraint(name = "uk_plan_saas_code", columnNames = "code")
)
@Getter
@Setter
@NoArgsConstructor
public class PlanSaas extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "code", nullable = false, length = 30, updatable = false)
    private PlanAbonnement code;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "description", length = 300)
    private String description;

    @Column(name = "montant_xof", nullable = false)
    private Integer montantXof;

    @Column(name = "duree_mois", nullable = false)
    private Integer dureeMois;

    @Column(name = "actif", nullable = false)
    private Boolean actif = true;

    @Column(name = "featured", nullable = false)
    private Boolean featured = false;

    @Column(name = "ordre_affichage", nullable = false)
    private Integer ordreAffichage = 0;
}
