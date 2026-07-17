package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.utils.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "doyenne")
@NoArgsConstructor
public class Doyenne extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "nom", nullable = false, length = 400)
    private String nom;

    @Column(name = "description", length = 500)
    private String description;

    @JsonIgnore
    @OneToMany(mappedBy = "doyenne", fetch = FetchType.LAZY)
    private List<Paroisse> paroisses = new ArrayList<>();

    public Doyenne(Long id, UUID publicId, String nom, String description, List<Paroisse> paroisses) {
        this.id = id;
        this.publicId = publicId;
        this.nom = nom;
        this.description = description;
        this.paroisses = paroisses;
    }
}
