package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.enums.StatutReversement;
import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "demande_reversement")
@Getter
@Setter
@Filter(
        name = "tenantFilter",
        condition = "{reversement}.paroisse_id = :tenantId",
        deduceAliasInjectionPoints = false,
        aliases = @SqlFragmentAlias(alias = "reversement", table = "demande_reversement")
)
public class DemandeReversement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paroisse_id", nullable = false)
    private Paroisse paroisse;

    @Column(name = "montant", nullable = false)
    private Integer montant;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    private StatutReversement statut = StatutReversement.EN_ATTENTE;

    @Column(name = "nom_banque", length = 120)
    private String nomBanque;

    @Column(name = "titulaire_compte", length = 150)
    private String titulaireCompte;

    @Column(name = "iban_or_rib", length = 80)
    private String ibanOrRib;

    @Column(name = "motif", length = 500)
    private String motif;

    @Column(name = "reference_virement", length = 120)
    private String referenceVirement;

    @Column(name = "traite_par", length = 120)
    private String traitePar;

    @Column(name = "traite_at")
    private LocalDateTime traiteAt;
}
