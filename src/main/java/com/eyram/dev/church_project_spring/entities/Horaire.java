package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name ="horaire")
@Getter
@Setter
@NoArgsConstructor
public class Horaire extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Enumerated(EnumType.STRING) // persiste "MONDAY", "TUESDAY"... au lieu d'un int
    @Column(name = "jour_semaine", nullable = false, length = 16)
    private DayOfWeek jourSemaine;

    @Column(name = "heure", nullable = false)
    private LocalTime heure;

    @Column(name = "status_del", nullable = false)
    private Boolean statusDel = false;

    public Horaire(Long id, UUID publicId, DayOfWeek jourSemaine, LocalTime heure, Boolean statusDel) {
        this.id = id;
        this.publicId = publicId;
        this.jourSemaine = jourSemaine;
        this.heure = heure;
        this.statusDel = statusDel != null ? statusDel : false;
    }

    @Override
    public String toString() {
        return "Horaire{" +
                "id=" + id +
                ", publicId=" + publicId +
                ", jourSemaine=" + jourSemaine +
                ", heure=" + heure +
                ", statusDel=" + statusDel +
                '}';
    }
}
