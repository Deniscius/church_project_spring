package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.utils.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;


import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "localite")
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

    @JsonIgnore
    @OneToMany(mappedBy = "localite")
    private List<Paroisse> paroisses;

    public Localite(Long id, UUID publicId, String quartier, String ville, List<Paroisse> paroisses) {
        this.id = id;
        this.publicId = publicId;
        this.quartier = quartier;
        this.ville = ville;
        this.paroisses = paroisses;
    }

    public Localite() {

    }


}
