package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.enums.RoleParoisse;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "paroisse_inscription_membre")
@Getter
@Setter
public class ParoisseInscriptionMembre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inscription_id", nullable = false)
    private ParoisseInscription inscription;

    @Column(name = "nom", nullable = false, length = 80)
    private String nom;

    @Column(name = "prenom", nullable = false, length = 80)
    private String prenom;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "telephone", length = 50)
    private String telephone;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_paroisse", nullable = false, length = 40)
    private RoleParoisse roleParoisse;

    @Column(name = "username", length = 80)
    private String username;
}
