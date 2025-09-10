package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "type_demande")
public class TypeDemande extends BaseEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", unique = true, updatable = false, nullable = false)
    private UUID publicId;

    @Column(name = "libelle", length = 100)
    private String libelle;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "status_del")
    private Boolean statusDel = false;

    @OneToMany(mappedBy = "typeDemande", cascade = CascadeType.ALL)
    private List<Demande> demandes;

    public TypeDemande(Long id, UUID publicId, String libelle, String description, Boolean statusDel, List<Demande> demandes) {
        this.id = id;
        this.publicId = publicId;
        this.libelle = libelle;
        this.description = description;
        this.statusDel = statusDel;
        this.demandes = demandes;
    }

    public TypeDemande() {}

    @Override
    public String toString() {
        return "TypeDemande{" +
                "id=" + id +
                ", publicId=" + publicId +
                ", libelle='" + libelle + '\'' +
                ", description='" + description + '\'' +
                ", statusDel=" + statusDel +
                '}';
    }
}
