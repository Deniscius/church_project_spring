package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.enums.ModePaiement;
import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;
import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "details_paiement")
@Getter
@Setter
public class DetailsPaiement extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "date_details_paiement", nullable = false)
    private LocalDateTime dateDetailsPaiement;

    @Column(name = "montant", nullable = false)
    private Integer montant;

    @Column(name = "id_transaction", length = 150)
    private String idTransaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_paiement", nullable = false)
    private StatutPaiementEnum statutPaiement;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement", nullable = false)
    private ModePaiement modePaiement;

    @Column(name = "numero", length = 30)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_paiement_id", nullable = false)
    private TypePaiement typePaiement;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_id", unique = true, nullable = false)
    private Facture facture;
}