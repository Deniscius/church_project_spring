package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.enums.PlanAbonnement;
import com.eyram.dev.church_project_spring.enums.StatutAbonnement;
import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "paroisse_abonnement")
@Getter
@Setter
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

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false, length = 30)
    private PlanAbonnement plan;

    @Column(name = "montant", nullable = false)
    private Integer montant;

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
