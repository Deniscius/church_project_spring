package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;


import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "localite")
@Getter
@Setter
public class Localite extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id")
    private UUID publicId;

    @Column(name = "quartier", length = 200)
    private String quartier;

    @Column(name = "ville")
    private String ville;

    @Column(name = "status_del")
    private Boolean statusDel = false;

    public Localite(Long id, UUID publicId, String quartier, String ville, Boolean statusDel) {
        this.id = id;
        this.publicId = publicId;
        this.quartier = quartier;
        this.ville = ville;
        this.statusDel = statusDel;
    }

    public Localite() {

    }

    @Override
    public String toString() {
        return "Localite{" +
                "id=" + id +
                ", publicId='" + publicId + '\'' +
                ", quartier='" + quartier + '\'' +
                ", ville='" + ville + '\'' +
                ", statusDel=" + statusDel +
                '}';
    }
}
