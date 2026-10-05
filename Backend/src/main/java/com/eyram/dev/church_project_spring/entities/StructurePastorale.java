package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.enums.TypeStructurePastorale;
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
@Table(name = "structure_pastorale")
@Filter(
        name = "tenantFilter",
        condition = "{structure_pastorale}.paroisse_id = :tenantId",
        deduceAliasInjectionPoints = false,
        aliases = @SqlFragmentAlias(alias = "structure_pastorale", table = "structure_pastorale")
)
@Getter
@Setter
@NoArgsConstructor
public class StructurePastorale extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private TypeStructurePastorale type;

    @Column(name = "nom", nullable = false, length = 150)
    private String nom;

    @Column(name = "attributions", length = 1500)
    private String attributions;

    @Column(name = "ordre_affichage", nullable = false)
    private Integer ordreAffichage = 0;

    @Column(name = "actif", nullable = false)
    private Boolean actif = true;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paroisse_id", nullable = false)
    private Paroisse paroisse;
}
