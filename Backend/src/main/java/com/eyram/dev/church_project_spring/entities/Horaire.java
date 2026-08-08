package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.enums.JourSemaine;
import com.eyram.dev.church_project_spring.enums.NatureForfaitEnum;
import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "horaire")
@Filter(
        name = "tenantFilter",
        condition = "{horaire}.paroisse_id = :tenantId",
        deduceAliasInjectionPoints = false,
        aliases = @SqlFragmentAlias(alias = "horaire", table = "horaire")
)
@Getter
@Setter
public class Horaire extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "jour_semaine", nullable = false)
    private JourSemaine jourSemaine;

    @Column(name = "heure_celebration", nullable = false)
    private LocalTime heureCelebration;

    @Column(name = "libelle", length = 150)
    private String libelle;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Si renseigné : créneau ponctuel pour cette date uniquement
     * (sinon créneau hebdomadaire récurrent).
     */
    @Column(name = "date_specifique")
    private LocalDate dateSpecifique;

    /**
     * Si true (implique {@code dateSpecifique}) : ce jour-là, seule cette messe
     * est au programme de la paroisse (les créneaux hebdomadaires sont masqués).
     */
    @Column(name = "unique_sur_paroisse", nullable = false)
    private Boolean uniqueSurParoisse = false;

    /**
     * Honoraire (nature de forfait) applicable aux intentions sur cette date précise.
     * Typiquement {@code SPECIALE} pour un événement solennel ; sinon NORMALE / DOMINICALE
     * selon le jour choisi par l'admin.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "nature_honoraire", length = 20)
    private NatureForfaitEnum natureHonoraire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paroisse_id", nullable = false)
    private Paroisse paroisse;

    public Horaire(Long id, UUID publicId, JourSemaine jourSemaine, LocalTime heureCelebration, String libelle, Boolean isActive, Paroisse paroisse) {
        this.id = id;
        this.publicId = publicId;
        this.jourSemaine = jourSemaine;
        this.heureCelebration = heureCelebration;
        this.libelle = libelle;
        this.isActive = isActive;
        this.paroisse = paroisse;
    }

    public Horaire() {
        super();

    }
}
