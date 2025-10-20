package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.enums.DureeMesseEnum;
import com.eyram.dev.church_project_spring.enums.TypeDemandeEnum;
import com.eyram.dev.church_project_spring.utils.BaseEntity;
import com.eyram.dev.church_project_spring.enums.StatusValidationEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "demande")
@Getter @Setter
public class Demande extends BaseEntity implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "intention", length = 500)
    private String intention;

    @Column(name = "date_demande", length = 100)
    private LocalDate dateDemande;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_validation")
    private StatusValidationEnum statusValidationEnum;


    @Enumerated(EnumType.STRING)
    @Column(name = "type_demande", nullable = false)
    private TypeDemandeEnum typeDemandeEnum;


    @Enumerated(EnumType.STRING)
    @Column(name = "duree_messe", nullable = false)
    private DureeMesseEnum dureeMesse;


    @ElementCollection
    @CollectionTable(name = "demande_dates", joinColumns = @JoinColumn(name = "demande_id"))
    @Column(name = "jour_cible", nullable = false)
    private java.util.List<LocalDate> dates;

    @Column(name = "prix_total", nullable = false)
    private Integer prixTotal;

    @Column(name = "status_del", nullable = false)
    private Boolean statusDel = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fidele_id", nullable = false)
    private Fidele fidele;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "type_demande_id")
    private TypeDemande typeDemande;


    public Demande(Long id, UUID publicId, String intention, LocalDate dateDemande, StatusValidationEnum statusValidationEnum, TypeDemandeEnum typeDemandeEnum, DureeMesseEnum dureeMesse, List<LocalDate> dates, Integer prixTotal, Boolean statusDel, Fidele fidele, TypeDemande typeDemande) {
        this.id = id;
        this.publicId = publicId;
        this.intention = intention;
        this.dateDemande = dateDemande;
        this.statusValidationEnum = statusValidationEnum;
        this.typeDemandeEnum = typeDemandeEnum;
        this.dureeMesse = dureeMesse;
        this.dates = dates;
        this.prixTotal = prixTotal;
        this.statusDel = statusDel;
        this.fidele = fidele;
        this.typeDemande = typeDemande;
    }


    @Override
    public String toString() {
        return "Demande{" +
                "id=" + id +
                ", publicId=" + publicId +
                ", intention='" + intention + '\'' +
                ", dateDemande=" + dateDemande +
                ", statusValidationEnum=" + statusValidationEnum +
                ", typeDemandeEnum=" + typeDemandeEnum +
                ", dureeMesse=" + dureeMesse +
                ", dates=" + dates +
                ", prixTotal=" + prixTotal +
                ", statusDel=" + statusDel +
                ", fidele=" + fidele +
                ", typeDemande=" + typeDemande +
                '}';
    }

    public Demande() {

    }
}
