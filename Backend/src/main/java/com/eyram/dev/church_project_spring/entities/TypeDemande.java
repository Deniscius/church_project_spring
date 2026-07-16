package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.enums.TypeDemandeEnum;
import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(
        name = "type_demande",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"libelle", "paroisse_id", "type_principal"})
        }
)
@Filter(
        name = "tenantFilter",
        condition = "{typeDemande}.paroisse_id = :tenantId",
        deduceAliasInjectionPoints = false,
        aliases = @SqlFragmentAlias(alias = "typeDemande", table = "type_demande")
)
@Getter
@Setter
@NoArgsConstructor
public class TypeDemande extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "libelle", nullable = false, length = 150)
    private String libelle;

    @Column(name = "description", length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_principal", nullable = false, length = 50)
    private TypeDemandeEnum typeDemandeEnum;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paroisse_id", nullable = false)
    private Paroisse paroisse;
}
