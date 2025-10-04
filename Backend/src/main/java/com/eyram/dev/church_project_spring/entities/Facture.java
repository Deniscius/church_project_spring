package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name="facture")
@Data
public class Facture extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id")
    private UUID publicId;

    @Column(name = "date_paiement")
    private LocalDate datePaiement;

    @Column(name = "montant")
    private Double montant;

    @Column(name = "date_production")
    private LocalDate dateProduction;

    @Column(name = "status_del")
    private Boolean statusDel = false;


    public Facture(Long id, UUID publicId, LocalDate datePaiement, Double montant, LocalDate dateProduction, Boolean statusDel) {
        this.id = id;
        this.publicId = publicId;
        this.datePaiement = datePaiement;
        this.montant = montant;
        this.dateProduction = dateProduction;
        this.statusDel = statusDel;
    }

    public Facture() {

    }

}
