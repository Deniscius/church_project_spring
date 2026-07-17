package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SqlFragmentAlias;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "demande_date",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"demande_id", "ordre"}),
                @UniqueConstraint(columnNames = {"demande_id", "date_celebration"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@Filter(
        name = "tenantFilter",
        condition = "exists (select 1 from demande d where d.id = {demandeDate}.demande_id and d.paroisse_id = :tenantId)",
        deduceAliasInjectionPoints = false,
        aliases = @SqlFragmentAlias(alias = "demandeDate", table = "demande_date")
)
public class DemandeDate extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "ordre", nullable = false)
    private Integer ordre;

    @Column(name = "date_celebration", nullable = false)
    private LocalDate dateCelebration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_id", nullable = false)
    private Demande demande;
}
