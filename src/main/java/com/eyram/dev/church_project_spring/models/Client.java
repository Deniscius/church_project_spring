package com.eyram.dev.church_project_spring.models;


import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table (name = "client")

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

    @Column(name = "status")
    private boolean status = true;

    public Client() {

    }

    public Client(Long id, UUID publicId, String nom, String prenoms, String tel, boolean status) {
        this.id = id;
        this.publicId = publicId;
        this.nom = nom;
        this.prenoms = prenoms;
        this.tel = tel;
        this.status = status;
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

    public UUID getPublicId() {
        return publicId;
    }

    public void setPublicId(UUID publicId) {
        this.publicId = publicId;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Client{" +
                "id=" + id +
                ", publicId=" + publicId +
                ", nom='" + nom + '\'' +
                ", prenoms='" + prenoms + '\'' +
                ", tel='" + tel + '\'' +
                ", status=" + status +
                '}';
    }
}
