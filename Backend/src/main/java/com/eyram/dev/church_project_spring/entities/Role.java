package com.eyram.dev.church_project_spring.entities;


import com.eyram.dev.church_project_spring.enums.RoleEnum;
import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;

@Entity
@Getter
@Setter
@Table(name = "role")
public class Role extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", unique = true, nullable = false)
    private String publicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "nom_role")
    private RoleEnum role;

    @Column(name ="description")
    private String description;

    @Column(name = "status_del")
    private Boolean statusDel = false;


    public Role(Long id, String publicId, RoleEnum role, String description) {
        this.id = id;
        this.publicId = publicId;
        this.role = role;
        this.description = description;
    }
public Role() {

    }

    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", publicId='" + publicId + '\'' +
                ", role=" + role +
                ", description='" + description + '\'' +
                '}';
    }
}
