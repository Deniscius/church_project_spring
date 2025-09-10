package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "paroisse")
@Getter
@Setter
public class Paroisse extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", nullable = false)
    private UUID publicId;

    @Column(name = "nom", length = 50)
    private String nom ;


    @Column(name = "tel", length = 50)
    private String tel ;

    @Column(name = "status_del")
    private Boolean statusDel = false;

    public Paroisse(Long id, UUID publicId, String nom, String tel, boolean statusDel) {
        this.id = id;
        this.publicId = publicId;
        this.nom = nom;
        this.tel = tel;
        this.statusDel = statusDel;
    }


    public Paroisse() {

    }

    @Override
    public String toString() {
        return "Paroisse{" +
                "id=" + id +
                ", publicId=" + publicId +
                ", nom='" + nom + '\'' +
                ", tel='" + tel + '\'' +
                ", statusDel=" + statusDel +
                '}';
    }
}
