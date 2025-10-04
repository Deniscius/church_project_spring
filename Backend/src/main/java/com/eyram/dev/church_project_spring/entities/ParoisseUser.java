package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.service.TypePaiementService;
import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.boot.autoconfigure.web.WebProperties;

import java.io.Serializable;

@Entity
@Data
@Table(name = "paroisse_user")
public class ParoisseUser extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", unique = true, nullable = false)
    private String publicId;


    @Column(name = "username")
    private String username;


    @Column(name = "password")
    private String password;

    @Column(name = "status_del")
    private Boolean statusDel = false;

    public ParoisseUser(){

    }
}
