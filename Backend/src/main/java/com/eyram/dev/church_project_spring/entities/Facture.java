package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;
import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "facture")
@Getter
@Setter
@Filter(
        name = "tenantFilter",
        condition = "exists (select 1 from demande d where d.id = {facture}.demande_id and d.paroisse_id = :tenantId)",
        deduceAliasInjectionPoints = false,
        aliases = @SqlFragmentAlias(alias = "facture", table = "facture")
)
public class Facture extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "ref_facture", unique = true, nullable = false, length = 100)
    private String refFacture;

    @Column(name = "date_paiement")
    private LocalDateTime datePaiement;

    @Column(name = "montant", nullable = false)
    private Integer montant;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_paiement", nullable = false)
    private StatutPaiementEnum statutPaiement;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_id", unique = true, nullable = false)
    private Demande demande;
}
