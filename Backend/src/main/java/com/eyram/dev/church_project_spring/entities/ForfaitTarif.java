package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SqlFragmentAlias;

import com.eyram.dev.church_project_spring.enums.JourSemaine;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "forfait_tarif",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"code_forfait"}),
                @UniqueConstraint(columnNames = {"nom_forfait", "type_demande_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@Filter(
        name = "tenantFilter",
        condition = "exists (select 1 from type_demande td where td.id = {forfaitTarif}.type_demande_id and td.paroisse_id = :tenantId)",
        deduceAliasInjectionPoints = false,
        aliases = @SqlFragmentAlias(alias = "forfaitTarif", table = "forfait_tarif")
)
public class ForfaitTarif extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "code_forfait", nullable = false, unique = true, length = 50)
    private String codeForfait;

    @Column(name = "nom_forfait", nullable = false, length = 150)
    private String nomForfait;

    @Column(name = "montant_forfait", nullable = false, precision = 12, scale = 2)
    private BigDecimal montantForfait;

    @Column(name = "nombre_jour")
    private Integer nombreJour;

    @Column(name = "nombre_celebration")
    private Integer nombreCelebration;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "forfait_tarif_jour_autorise",
            joinColumns = @JoinColumn(name = "forfait_tarif_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "jour_semaine", nullable = false, length = 20)
    private Set<JourSemaine> joursCelebrationAutorises = new HashSet<>();

    @Column(name = "heure_personnalise", nullable = false)
    private Boolean heurePersonnalise = false;

    @Column(name = "libelle", length = 255)
    private String libelle;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_demande_id", nullable = false)
    private TypeDemande typeDemande;
}
