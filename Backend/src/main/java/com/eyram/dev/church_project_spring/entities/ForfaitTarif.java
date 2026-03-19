package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
import java.math.BigDecimal;
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

    @Column(name = "jours_autorise")
    private Integer joursAutorise;

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