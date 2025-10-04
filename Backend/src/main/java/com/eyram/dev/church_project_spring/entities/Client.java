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

@Entity
@Table (name = "client")
@Getter
@Setter
@Data
public class Client extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column (name ="public_id", unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @Column (name ="nom", length = 100 )
    private String nom;

    @Column (name ="prenoms", length = 100 )
    private String prenoms;

    @Column (name ="tel", length = 100 )
    private String tel;

    @Column(name = "status_del")
    private Boolean statusDel = false;

    public Client() {

    }

    public Client(Long id, UUID publicId, String nom, String prenoms, String tel, boolean statusDel, List<Demande> demandes) {
        this.id = id;
        this.publicId = publicId;
        this.nom = nom;
        this.prenoms = prenoms;
        this.tel = tel;
        this.statusDel = statusDel;
        this.demandes = demandes;
    }

    @JsonIgnore
    @OneToMany(mappedBy = "client")
    private List<Demande> demandes;



    @Override
    public String toString() {
        return "Client{" +
                "id=" + id +
                ", publicId=" + publicId +
                ", nom='" + nom + '\'' +
                ", prenoms='" + prenoms + '\'' +
                ", tel='" + tel + '\'' +
                ", statusDel=" + statusDel +
                ", demandes=" + demandes +
                '}';
    }
}
