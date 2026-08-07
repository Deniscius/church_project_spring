package com.eyram.dev.church_project_spring.service.tenant;

import com.eyram.dev.church_project_spring.entities.ForfaitTarif;
import com.eyram.dev.church_project_spring.entities.Horaire;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.TypeDemande;
import com.eyram.dev.church_project_spring.enums.JourSemaine;
import com.eyram.dev.church_project_spring.repositories.ForfaitTarifRepository;
import com.eyram.dev.church_project_spring.repositories.HoraireRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.repositories.TypeDemandeRepository;
import com.eyram.dev.church_project_spring.utils.BusinessCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Clone le catalogue plateforme (horaires, types, forfaits) vers les nouveaux
 * tenants. Le support technique est la paroisse {@code is_system = true} —
 * ce n'est pas une paroisse cliente.
 *
 * <p>Les horaires sont clonés avant les types : une demande n'est exploitable
 * que si la paroisse propose au moins une heure de célébration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantCatalogBootstrapService {

    private final ParoisseRepository paroisseRepository;
    private final HoraireRepository horaireRepository;
    private final TypeDemandeRepository typeDemandeRepository;
    private final ForfaitTarifRepository forfaitTarifRepository;

    @Transactional
    public int seedDefaultsIfEmpty(Paroisse target) {
        if (target == null || target.getId() == null || Boolean.TRUE.equals(target.getIsSystem())) {
            return 0;
        }

        Optional<Paroisse> templateOpt = resolveTemplate();
        if (templateOpt.isEmpty()) {
            log.warn(
                    "Catalogue tenant non initialisé pour « {} » : catalogue plateforme introuvable",
                    target.getNom()
            );
            return 0;
        }

        Paroisse template = templateOpt.get();
        if (template.getId().equals(target.getId())) {
            return 0;
        }

        int seeded = seedHoraires(template, target) + seedTypesAndForfaits(template, target);
        if (seeded > 0) {
            log.info(
                    "Catalogue plateforme cloné vers « {} » : {} élément(s)",
                    target.getNom(),
                    seeded
            );
        }
        return seeded;
    }

    /**
     * Filet de sécurité pour les tenants déjà engagés commercialement
     * ({@code ACTIVE}, {@code EN_ATTENTE_PAIEMENT}, {@code EN_TOLERANCE}) dont
     * le catalogue serait encore vide. Les prospects restent sans catalogue
     * jusqu'à l'inscription / activation.
     */
    @Transactional
    public int backfillEmptyTenants() {
        if (resolveTemplate().isEmpty()) {
            log.warn("Backfill catalogue ignoré : catalogue plateforme introuvable");
            return 0;
        }

        int seeded = 0;
        for (Paroisse paroisse : paroisseRepository.findAllByStatusDelFalseAndIsSystemFalseOrderByNomAsc()) {
            if (paroisse.getStatutTenant() == null) {
                continue;
            }
            switch (paroisse.getStatutTenant()) {
                case ACTIVE, EN_ATTENTE_PAIEMENT, EN_TOLERANCE ->
                        seeded += seedDefaultsIfEmpty(paroisse);
                default -> {
                    // Prospect / suspendue / résiliée : clonage à l'activation.
                }
            }
        }
        if (seeded > 0) {
            log.info("Backfill catalogue tenant : {} élément(s) clonés au total", seeded);
        }
        return seeded;
    }

    private int seedHoraires(Paroisse template, Paroisse target) {
        if (!horaireRepository.findByParoisseAndStatusDelFalse(target).isEmpty()) {
            return 0;
        }

        List<Horaire> sources = horaireRepository.findByParoisseAndStatusDelFalse(template);
        if (sources.isEmpty()) {
            log.warn(
                    "Catalogue plateforme sans horaire — « {} » démarre sans heure de célébration",
                    target.getNom()
            );
            return 0;
        }

        int cloned = 0;
        for (Horaire source : sources) {
            Horaire copy = new Horaire();
            copy.setJourSemaine(source.getJourSemaine());
            copy.setHeureCelebration(source.getHeureCelebration());
            copy.setLibelle(source.getLibelle());
            copy.setIsActive(source.getIsActive() == null || source.getIsActive());
            copy.setParoisse(target);
            copy.setStatusDel(false);
            horaireRepository.save(copy);
            cloned++;
        }
        return cloned;
    }

    private int seedTypesAndForfaits(Paroisse template, Paroisse target) {
        if (!typeDemandeRepository.findByParoisseAndStatusDelFalse(target).isEmpty()) {
            return 0;
        }

        List<TypeDemande> sourceTypes = typeDemandeRepository.findByParoisseAndStatusDelFalse(template);
        if (sourceTypes.isEmpty()) {
            log.warn(
                    "Catalogue plateforme sans type de demande — rien à cloner pour « {} »",
                    target.getNom()
            );
            return 0;
        }

        int cloned = 0;
        for (TypeDemande source : sourceTypes) {
            TypeDemande copy = cloneType(source, target);
            typeDemandeRepository.save(copy);
            cloned++;

            for (ForfaitTarif sourceForfait : forfaitTarifRepository.findByTypeDemandeAndStatusDelFalse(source)) {
                forfaitTarifRepository.save(cloneForfait(sourceForfait, copy));
                cloned++;
            }
        }
        return cloned;
    }

    /** Support technique du catalogue plateforme (exposé au comptable). */
    @Transactional(readOnly = true)
    public Optional<Paroisse> findTemplateParoisse() {
        return resolveTemplate();
    }

    @Transactional(readOnly = true)
    public boolean isTemplateParoisse(Paroisse paroisse) {
        return paroisse != null && Boolean.TRUE.equals(paroisse.getIsSystem());
    }

    private Optional<Paroisse> resolveTemplate() {
        return paroisseRepository.findFirstByIsSystemTrueAndStatusDelFalse();
    }

    private TypeDemande cloneType(TypeDemande source, Paroisse target) {
        TypeDemande copy = new TypeDemande();
        copy.setLibelle(source.getLibelle());
        copy.setDescription(source.getDescription());
        copy.setTypeDemandeEnum(source.getTypeDemandeEnum());
        copy.setIsActive(source.getIsActive() == null || source.getIsActive());
        copy.setDelaiMinimumHeures(
                source.getDelaiMinimumHeures() != null ? source.getDelaiMinimumHeures() : 24
        );
        copy.setParoisse(target);
        copy.setStatusDel(false);
        copy.setJoursCelebrationAutorises(copyJours(source.getJoursCelebrationAutorises()));
        return copy;
    }

    private ForfaitTarif cloneForfait(ForfaitTarif source, TypeDemande targetType) {
        ForfaitTarif copy = new ForfaitTarif();
        copy.setNomForfait(source.getNomForfait());
        copy.setNatureForfait(source.getNatureForfait());
        copy.setMontantForfait(source.getMontantForfait());
        copy.setNombreJour(source.getNombreJour());
        copy.setNombreCelebration(source.getNombreCelebration());
        copy.setHeurePersonnalise(Boolean.TRUE.equals(source.getHeurePersonnalise()));
        copy.setLibelle(source.getLibelle());
        copy.setIsActive(source.getIsActive() == null || source.getIsActive());
        copy.setTypeDemande(targetType);
        copy.setStatusDel(false);
        copy.setJoursCelebrationAutorises(copyJours(source.getJoursCelebrationAutorises()));

        String parishName = targetType.getParoisse() != null ? targetType.getParoisse().getNom() : null;
        copy.setCodeForfait(BusinessCodeGenerator.unique(
                BusinessCodeGenerator.forfaitCode(
                        parishName,
                        targetType.getTypeDemandeEnum(),
                        source.getNatureForfait(),
                        source.getNombreCelebration(),
                        source.getNombreJour()
                ),
                forfaitTarifRepository::existsByCodeForfaitAndStatusDelFalse
        ));
        return copy;
    }

    private Set<JourSemaine> copyJours(Set<JourSemaine> source) {
        return source == null ? new HashSet<>() : new HashSet<>(source);
    }
}
