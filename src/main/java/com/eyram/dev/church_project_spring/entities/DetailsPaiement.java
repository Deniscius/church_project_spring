package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "details_paiement")
@Data
public class DetailsPaiement extends BaseEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", nullable = false)
    private UUID publicId;

    @Column(name = "date_details_paiement")
    private LocalDate dateDetailsPaiement;

    @Column(name = "montant")
    private Double montant;

    @Column(name = "status_del")
    private Boolean statusDel = false;

    public DetailsPaiement() {

    }

}


