package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(
        name = "demande_date",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"demande_id", "ordre"}),
                @UniqueConstraint(columnNames = {"demande_id", "date_celebration"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@Filter(
        name = "tenantFilter",
        condition = "exists (select 1 from demande d where d.id = {demandeDate}.demande_id and d.paroisse_id = :tenantId)",
        deduceAliasInjectionPoints = false,
        aliases = @SqlFragmentAlias(alias = "demandeDate", table = "demande_date")
)
public class DemandeDate extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "ordre", nullable = false)
    private Integer ordre;

    @Column(name = "date_celebration", nullable = false)
    private LocalDate dateCelebration;

    /** Créneau paroissial propre à cette date (multi-jours : peut différer selon le jour). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "horaire_id")
    private Horaire horaire;

    /** Heure personnalisée propre à cette date (si le forfait l'autorise). */
    @Column(name = "heure_personnalisee")
    private LocalTime heurePersonnalisee;

    /** True dès que la célébration de ce créneau est confirmée (manuel ou auto après l'heure). */
    @Column(name = "celebre", nullable = false)
    private Boolean celebre = Boolean.FALSE;

    @Column(name = "celebre_at")
    private LocalDateTime celebreAt;

    /** Dernier rappel J-1 (fidèle + paroisse). */
    @Column(name = "last_reminder_j1_at")
    private LocalDateTime lastReminderJ1At;

    /** Dernier rappel H-2 (fidèle). */
    @Column(name = "last_reminder_h2_at")
    private LocalDateTime lastReminderH2At;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_id", nullable = false)
    private Demande demande;
}
