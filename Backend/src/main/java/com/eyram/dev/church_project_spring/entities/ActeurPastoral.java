package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.enums.CategorieActeurPastoral;
import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "acteur_pastoral")
@Filter(
        name = "tenantFilter",
        condition = "{acteur_pastoral}.paroisse_id = :tenantId",
        deduceAliasInjectionPoints = false,
        aliases = @SqlFragmentAlias(alias = "acteur_pastoral", table = "acteur_pastoral")
)
@Getter
@Setter
@NoArgsConstructor
public class ActeurPastoral extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "prenoms", nullable = false, length = 150)
    private String prenoms;

    @Column(name = "appellation", length = 50)
    private String appellation;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie", nullable = false, length = 30)
    private CategorieActeurPastoral categorie;

    @Column(name = "telephone", length = 50)
    private String telephone;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "actif", nullable = false)
    private Boolean actif = true;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paroisse_id", nullable = false)
    private Paroisse paroisse;
}
