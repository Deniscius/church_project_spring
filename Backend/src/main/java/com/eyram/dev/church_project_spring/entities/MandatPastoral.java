package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "mandat_pastoral")
@Filter(
        name = "tenantFilter",
        condition = "{mandat_pastoral}.paroisse_id = :tenantId",
        deduceAliasInjectionPoints = false,
        aliases = @SqlFragmentAlias(alias = "mandat_pastoral", table = "mandat_pastoral")
)
@Getter
@Setter
@NoArgsConstructor
public class MandatPastoral extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "fonction", nullable = false, length = 120)
    private String fonction;

    @Column(name = "attributions", length = 1500)
    private String attributions;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(name = "ordre_affichage", nullable = false)
    private Integer ordreAffichage = 0;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paroisse_id", nullable = false)
    private Paroisse paroisse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "annee_pastorale_id", nullable = false)
    private AnneePastorale anneePastorale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acteur_pastoral_id", nullable = false)
    private ActeurPastoral acteurPastoral;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "structure_pastorale_id", nullable = false)
    private StructurePastorale structurePastorale;
}
