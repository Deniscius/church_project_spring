package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.enums.StatutDemandeEnum;
import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;
import com.eyram.dev.church_project_spring.enums.StatutValidationEnum;
import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "demande")
@Getter
@Setter
@NoArgsConstructor
public class Demande extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "intention", nullable = false, length = 500)
    private String intention;

    @Column(name = "code_suivie", nullable = false, unique = true, length = 100)
    private String codeSuivie;

    @Column(name = "nom_fidele", nullable = false, length = 100)
    private String nomFidele;

    @Column(name = "prenom_fidele", nullable = false, length = 100)
    private String prenomFidele;

    @Column(name = "tel_fidele", nullable = false, length = 30)
    private String telFidele;

    @Column(name = "email_fidele", length = 150)
    private String emailFidele;

    @Column(name = "montant", nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    @Column(name = "nom_coursier", length = 100)
    private String nomCoursier;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_paiement", nullable = false, length = 30)
    private StatutPaiementEnum statutPaiement = StatutPaiementEnum.NON_PAYE;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_validation", nullable = false, length = 30)
    private StatutValidationEnum statutValidation = StatutValidationEnum.EN_ATTENTE;

    @Column(name = "validate_by", length = 150)
    private String validateBy;

    @Column(name = "heure_personnalisee")
    private LocalTime heurePersonnalisee;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_demande", nullable = false, length = 30)
    private StatutDemandeEnum statutDemande = StatutDemandeEnum.EN_ATTENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paroisse_id", nullable = false)
    private Paroisse paroisse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_demande_id", nullable = false)
    private TypeDemande typeDemande;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forfait_tarif_id", nullable = false)
    private ForfaitTarif forfaitTarif;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "horaire_id")
    private Horaire horaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_paiement_id")
    private TypePaiement typePaiement;

}