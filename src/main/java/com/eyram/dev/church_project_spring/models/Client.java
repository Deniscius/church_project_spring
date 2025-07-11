package com.eyram.dev.church_project_spring.models;


import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.*;

import java.io.Serializable;

@Entity
@Table (name = "client")

public class Client extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column (name ="nom", length = 100 )
    private String nom;
    @Column (name ="prenoms", length = 100 )
    private String prenoms;

    @Column (name ="tel", length = 100 )
    private String tel;

    public Client() {

    }

    public Client(Long id, String nom, String prenoms, String tel) {
        this.id = id;
        this.nom = nom;
        this.prenoms = prenoms;
        this.tel = tel;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenoms() {
        return prenoms;
    }

    public void setPrenoms(String prenoms) {
        this.prenoms = prenoms;
    }

    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }

    @Override
    public String toString() {
        return "Client{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", prenoms='" + prenoms + '\'' +
                ", tel='" + tel + '\'' +
                '}';
    }
}
