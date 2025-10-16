package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.utils.BaseEntity;
import com.eyram.dev.church_project_spring.enums.StatusValidationEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "demande")
@Getter
@Setter
public class Demande extends BaseEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column (name = "public_id", unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @Column (name = "intention", length = 500)
    private String intention;

    @Column (name = "date_demande", length = 100)
    private LocalDate dateDemande;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_validation")
    private StatusValidationEnum statusValidationEnum;

    @Column(name = "status_del")
    private Boolean statusDel;

    @ManyToOne()
    @JoinColumn(name = "fidele", nullable = false)
    private Fidele fidele;

    @ManyToOne
    @JoinColumn(name = "type_demande_id", nullable = false)
    private TypeDemande typeDemande;

    @Override
    public String toString() {
        return "Demande{" +
                "id=" + id +
                ", publicId=" + publicId +
                ", intention='" + intention + '\'' +
                ", dateDemande=" + dateDemande +
                ", statusValidation=" + statusValidationEnum +
                ", statusDel=" + statusDel +
                ", fidele=" + fidele +
                ", typeDemande=" + typeDemande +
                '}';
    }

    public Demande(Long id, UUID publicId, String intention, LocalDate dateDemande, StatusValidationEnum statusValidationEnum, Boolean statusDel, Fidele fidele, TypeDemande typeDemande) {
        this.id = id;
        this.publicId = publicId;
        this.intention = intention;
        this.dateDemande = dateDemande;
        this.statusValidationEnum = statusValidationEnum;
        this.statusDel = statusDel;
        this.fidele = fidele;
        this.typeDemande = typeDemande;
    }

    public Demande() {

    }
}
