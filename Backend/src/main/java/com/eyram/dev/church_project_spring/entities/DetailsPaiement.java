package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;
import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SqlFragmentAlias;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "details_paiement")
@Getter
@Setter
@Filter(
        name = "tenantFilter",
        condition = "exists (select 1 from facture f join demande d on d.id = f.demande_id where f.id = {detailsPaiement}.facture_id and d.paroisse_id = :tenantId)",
        deduceAliasInjectionPoints = false,
        aliases = @SqlFragmentAlias(alias = "detailsPaiement", table = "details_paiement")
)
public class DetailsPaiement extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "date_details_paiement", nullable = false)
    private LocalDateTime dateDetailsPaiement;

    @Column(name = "montant", nullable = false)
    private Integer montant;

    /** Commission agrégateur (XOF). */
    @Column(name = "montant_frais", nullable = false)
    private Integer montantFrais = 0;

    @Column(name = "montant_frais_agregateur", nullable = false)
    private Integer montantFraisAgregeateur = 0;

    @Column(name = "montant_frais_plateforme", nullable = false)
    private Integer montantFraisPlateforme = 0;

    /** Montant effectivement débité chez le fidèle / envoyé à FedaPay. */
    @Column(name = "montant_charge")
    private Integer montantCharge;

    /** Montant net attendu pour la paroisse après frais. */
    @Column(name = "montant_net")
    private Integer montantNet;

    @Column(name = "provider", length = 40)
    private String provider;

    @Column(name = "payment_url", length = 500)
    private String paymentUrl;

    @Column(name = "id_transaction", length = 150, unique = true)
    private String idTransaction;

    @Column(name = "encaisseur_nom", length = 150)
    private String encaisseurNom;

    @Column(name = "encaisseur_user_public_id")
    private UUID encaisseurUserPublicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_paiement", nullable = false)
    private StatutPaiementEnum statutPaiement;

    @Column(name = "numero", length = 30)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_paiement_id", nullable = false)
    private TypePaiement typePaiement;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_id", unique = true, nullable = false)
    private Facture facture;
}
