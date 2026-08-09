package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "compte_paroisse")
@Getter
@Setter
@Filter(
        name = "tenantFilter",
        condition = "{compte}.paroisse_id = :tenantId",
        deduceAliasInjectionPoints = false,
        aliases = @SqlFragmentAlias(alias = "compte", table = "compte_paroisse")
)
public class CompteParoisse extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paroisse_id", nullable = false, unique = true)
    private Paroisse paroisse;

    @Column(name = "solde_disponible", nullable = false)
    private Integer soldeDisponible = 0;

    @Column(name = "solde_en_attente", nullable = false)
    private Integer soldeEnAttente = 0;
}
