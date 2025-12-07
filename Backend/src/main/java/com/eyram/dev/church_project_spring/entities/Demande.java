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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "intention", length = 500)
    private String intention;

    @Column(name = "date_demande", length = 100)
    private LocalDate dateDemande;

    @Column(name = "code_suivi", length = 8)
    private String code_suivi;

    @Column(name = "fidele_nom", length = 100)
    private String fdidele_nom;

    @Column(name = "fidele_prenom", length = 255)
    private String fidele_prenom;

    @Column(name = "fidele_tel", length = 15)
    private int fidele_tel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "type_demande_id", nullable = false)
    private TypeDemande typeDemande;


    public Demande(Long id, UUID publicId, String intention, LocalDate dateDemande, String code_suivi, String fdidele_nom, String fidele_prenom, int fidele_tel, TypeDemande typeDemande) {
        this.id = id;
        this.publicId = publicId;
        this.intention = intention;
        this.dateDemande = dateDemande;
        this.code_suivi = code_suivi;
        this.fdidele_nom = fdidele_nom;
        this.fidele_prenom = fidele_prenom;
        this.fidele_tel = fidele_tel;
        this.typeDemande = typeDemande;
    }

    public Demande(){

    }

    @Override
    public String toString() {
        return "Demande{" +
                "id=" + id +
                ", publicId=" + publicId +
                ", intention='" + intention + '\'' +
                ", dateDemande=" + dateDemande +
                ", code_suivi='" + code_suivi + '\'' +
                ", fdidele_nom='" + fdidele_nom + '\'' +
                ", fidele_prenom='" + fidele_prenom + '\'' +
                ", fidele_tel=" + fidele_tel +
                ", typeDemande=" + typeDemande +
                '}';
    }
}
