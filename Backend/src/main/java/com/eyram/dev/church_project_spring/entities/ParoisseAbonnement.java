package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.enums.StatutAbonnement;
import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "paroisse_abonnement")
@Getter
@Setter
@Filter(
        name = "tenantFilter",
        condition = "{abo}.paroisse_id = :tenantId",
        deduceAliasInjectionPoints = false,
        aliases = @SqlFragmentAlias(alias = "abo", table = "paroisse_abonnement")
)
public class ParoisseAbonnement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paroisse_id", nullable = false)
    private Paroisse paroisse;

    /** Code commercial du plan au moment de la création de la période. */
    @Column(name = "plan", nullable = false, length = 30)
    private String plan;

    /** Montant contractuel figé au moment de la création de l'abonnement. */
    @Column(name = "montant", nullable = false)
    private Integer montant;

    /** Durée contractuelle figée au moment de la création de l'abonnement. */
    @Column(name = "duree_mois", nullable = false)
    private Integer dureeMois;

    @Column(name = "debut_at")
    private LocalDateTime debutAt;

    @Column(name = "fin_at")
    private LocalDateTime finAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    private StatutAbonnement statut = StatutAbonnement.EN_ATTENTE;

    @Column(name = "id_transaction", length = 150)
    private String idTransaction;

    @Column(name = "payment_url", length = 500)
    private String paymentUrl;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    /** FEDAPAY | MANUEL */
    @Column(name = "activation_source", length = 30)
    private String activationSource;

    @Column(name = "activated_by_nom", length = 150)
    private String activatedByNom;
}
