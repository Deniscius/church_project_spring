package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.response.CelebrationIntentionResponse;
import com.eyram.dev.church_project_spring.DTO.response.CelebrationMesseGroupResponse;
import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.entities.DemandeDate;
import com.eyram.dev.church_project_spring.entities.ForfaitTarif;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.enums.StatutDemandeEnum;
import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;
import com.eyram.dev.church_project_spring.repositories.DemandeDateRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.service.storage.StoredFileService;
import com.eyram.dev.church_project_spring.utils.FideleNameUtils;
import com.eyram.dev.church_project_spring.utils.ForfaitDureeLabels;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Feuille d'intentions du jour pour la sacristie / le célébrant.
 * Regroupe les intentions par heure de messe (ex. 06:00, 08:00, 10:00).
 */
@Service
@RequiredArgsConstructor
public class FeuilleCelebrationService {

    private static final DateTimeFormatter DATE_LONG =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter DATE_SHORT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH' h 'mm");
    private static final Set<StatutDemandeEnum> STATUTS_FEUILLE = Set.of(
            StatutDemandeEnum.VALIDEE,
            StatutDemandeEnum.TERMINEE
    );

    private final DemandeDateRepository demandeDateRepository;
    private final ParoisseRepository paroisseRepository;
    private final TenantAccessService tenantAccessService;
    private final StoredFileService storedFileService;

    @Transactional(readOnly = true)
    public List<CelebrationMesseGroupResponse> listByParoisseAndDate(
            UUID paroissePublicId,
            LocalDate date,
            boolean inclureNonPayees,
            Collection<LocalTime> heures
    ) {
        Paroisse paroisse = requireAccessibleParoisse(paroissePublicId);
        return retainSelectedHours(
                groupByMesse(loadSorted(paroisse.getPublicId(), date, inclureNonPayees)),
                heures
        );
    }

    @Transactional(readOnly = true)
    public byte[] generatePdf(
            UUID paroissePublicId,
            LocalDate date,
            boolean inclureNonPayees,
            Collection<LocalTime> heures
    ) {
        Paroisse paroisse = requireAccessibleParoisse(paroissePublicId);
        List<CelebrationMesseGroupResponse> messes = retainSelectedHours(
                groupByMesse(loadSorted(paroisse.getPublicId(), date, inclureNonPayees)),
                heures
        );
        byte[] logo=null;
        if(org.springframework.util.StringUtils.hasText(paroisse.getLogoPath()))try{logo=java.nio.file.Files.readAllBytes(storedFileService.resolveAbsolute(paroisse.getLogoPath()));}catch(Exception ignored){}
        var branding=new com.missanye.printing.CelebrationSheetPdf.Branding(paroisse.getNom(),paroisse.getEmail(),paroisse.getTelephone(),logo);
        var groups=messes.stream().map(m->new com.missanye.printing.CelebrationSheetPdf.Mass(m.heure(),m.libelleMesse(),m.intentions().stream().map(i->new com.missanye.printing.CelebrationSheetPdf.Intention(i.intention(),fullName(i.demandeurPrenom(),i.demandeurNom()),i.progressionLabel(),i.celebre())).toList())).toList();
        return com.missanye.printing.CelebrationSheetPdf.render(branding,date,inclureNonPayees,heures==null?List.of():heures.stream().filter(java.util.Objects::nonNull).map(FeuilleCelebrationService::normalizeTime).distinct().sorted().toList(),groups);
    }

    /**
     * Restreint la feuille aux messes demandées. Une sélection vide vaut
     * « toutes les heures » : le sacristain imprime la journée entière.
     */
    private List<CelebrationMesseGroupResponse> retainSelectedHours(
            List<CelebrationMesseGroupResponse> messes,
            Collection<LocalTime> heures
    ) {
        if (heures == null || heures.isEmpty()) {
            return messes;
        }
        Set<LocalTime> wanted = heures.stream()
                .filter(java.util.Objects::nonNull)
                .map(FeuilleCelebrationService::normalizeTime)
                .collect(Collectors.toSet());
        if (wanted.isEmpty()) {
            return messes;
        }
        return messes.stream()
                .filter(messe -> wanted.contains(normalizeTime(messe.heure())))
                .toList();
    }

    private static String formatHeures(Collection<LocalTime> heures) {
        return heures.stream()
                .filter(java.util.Objects::nonNull)
                .map(FeuilleCelebrationService::normalizeTime)
                .distinct()
                .sorted()
                .map(TIME_FORMAT::format)
                .collect(Collectors.joining(", "));
    }

    private Paroisse requireAccessibleParoisse(UUID paroissePublicId) {
        Paroisse paroisse = paroisseRepository.findByPublicIdWithDoyenne(paroissePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));
        tenantAccessService.checkParoisseAccess(paroisse);
        return paroisse;
    }

    private List<DemandeDate> loadSorted(UUID paroissePublicId, LocalDate date, boolean inclureNonPayees) {
        List<DemandeDate> rows = demandeDateRepository.findForFeuilleCelebration(
                paroissePublicId,
                date,
                STATUTS_FEUILLE,
                inclureNonPayees,
                StatutPaiementEnum.PAYE
        );
        rows.sort(Comparator
                .comparing(this::resolveTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(dd -> dd.getDemande().getIntention(), Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(DemandeDate::getOrdre, Comparator.nullsLast(Comparator.naturalOrder())));
        return rows;
    }

    private Map<LocalTime, List<DemandeDate>> groupByTime(List<DemandeDate> rows) {
        Map<LocalTime, List<DemandeDate>> map = new LinkedHashMap<>();
        for (DemandeDate dd : rows) {
            map.computeIfAbsent(resolveTime(dd), key -> new ArrayList<>()).add(dd);
        }
        return map;
    }

    private List<CelebrationMesseGroupResponse> groupByMesse(List<DemandeDate> rows) {
        List<CelebrationMesseGroupResponse> groups = new ArrayList<>();
        for (Map.Entry<LocalTime, List<DemandeDate>> entry : groupByTime(rows).entrySet()) {
            LocalTime heure = entry.getKey();
            List<CelebrationIntentionResponse> intentions = entry.getValue().stream()
                    .map(this::toResponse)
                    .toList();
            groups.add(new CelebrationMesseGroupResponse(
                    heure,
                    resolveMesseLabel(heure, entry.getValue()),
                    intentions.size(),
                    intentions
            ));
        }
        return groups;
    }

    private String resolveMesseLabel(LocalTime heure, List<DemandeDate> rowsForTime) {
        if (heure == null) {
            return "Messe — heure non précisée";
        }
        String fromHoraire = rowsForTime.stream()
                .map(DemandeDate::getDemande)
                .filter(d -> d.getHoraire() != null
                        && d.getHoraire().getHeureCelebration() != null
                        && normalizeTime(d.getHoraire().getHeureCelebration()).equals(heure)
                        && d.getHoraire().getLibelle() != null
                        && !d.getHoraire().getLibelle().isBlank())
                .map(d -> d.getHoraire().getLibelle().trim())
                .findFirst()
                .orElse(null);
        if (fromHoraire != null) {
            return fromHoraire + " (" + heure.format(TIME_FORMAT) + ")";
        }
        return "Messe de " + heure.format(TIME_FORMAT);
    }

    private CelebrationIntentionResponse toResponse(DemandeDate dd) {
        Demande demande = dd.getDemande();
        ForfaitTarif forfait = demande.getForfaitTarif();
        Integer nombre = forfait != null ? forfait.getNombreCelebration() : null;
        String duree = ForfaitDureeLabels.labelFor(nombre);
        String horaireLibelle = demande.getHoraire() != null ? demande.getHoraire().getLibelle() : null;
        boolean celebre = Boolean.TRUE.equals(dd.getCelebre());
        long dejaCelebrees = demandeDateRepository.countCelebratedByDemandeId(demande.getId());

        return new CelebrationIntentionResponse(
                dd.getPublicId(),
                demande.getPublicId(),
                demande.getCodeSuivie(),
                dd.getDateCelebration(),
                demande.getCreatedAt(),
                dd.getOrdre(),
                nombre,
                duree,
                buildProgressionLabel(dd.getOrdre(), nombre, duree, (int) dejaCelebrees, celebre),
                resolveTime(dd),
                horaireLibelle,
                demande.getIntention(),
                demande.getTypeDemande() != null ? demande.getTypeDemande().getLibelle() : null,
                forfait != null ? forfait.getNomForfait() : null,
                forfait != null && forfait.getNatureForfait() != null ? forfait.getNatureForfait().name() : null,
                demande.getNomFidele(),
                demande.getPrenomFidele(),
                demande.getTelFidele(),
                demande.getEmailFidele(),
                demande.getStatutDemande() != null ? demande.getStatutDemande().name() : null,
                demande.getStatutPaiement() != null ? demande.getStatutPaiement().name() : null,
                celebre,
                dd.getCelebreAt()
        );
    }

    static String buildProgressionLabel(
            Integer ordre,
            Integer nombreCelebration,
            String dureeLabel,
            int dejaCelebrees,
            boolean currentCelebre
    ) {
        if (!ForfaitDureeLabels.isMultiCelebration(nombreCelebration) || ordre == null) {
            return currentCelebre ? "Célébrée" : null;
        }
        int deja = Math.max(0, dejaCelebrees);
        String ordinal = ordre == 1 ? "1ère" : ordre + "e";
        String label = dureeLabel != null ? capitalize(dureeLabel) : "Série";
        String status = currentCelebre ? " — terminée" : "";
        return label + " — " + ordinal + " célébration sur " + nombreCelebration
                + " (" + deja + " déjà célébrée" + (deja > 1 ? "s" : "") + ")" + status;
    }

    /** @deprecated conservé pour compatibilité des tests éventuels */
    @Deprecated(forRemoval = false)
    static String buildProgressionLabel(Integer ordre, Integer nombreCelebration, String dureeLabel) {
        int deja = ordre == null ? 0 : Math.max(0, ordre - 1);
        return buildProgressionLabel(ordre, nombreCelebration, dureeLabel, deja, false);
    }

    private LocalTime resolveTime(DemandeDate dd) {
        LocalTime raw = null;
        if (dd.getHeurePersonnalisee() != null) {
            raw = dd.getHeurePersonnalisee();
        } else if (dd.getHoraire() != null && dd.getHoraire().getHeureCelebration() != null) {
            raw = dd.getHoraire().getHeureCelebration();
        } else {
            Demande demande = dd.getDemande();
            if (demande.getHeurePersonnalisee() != null) {
                raw = demande.getHeurePersonnalisee();
            } else if (demande.getHoraire() != null && demande.getHoraire().getHeureCelebration() != null) {
                raw = demande.getHoraire().getHeureCelebration();
            }
        }
        return normalizeTime(raw);
    }

    private static LocalTime normalizeTime(LocalTime time) {
        return time == null ? null : time.withSecond(0).withNano(0);
    }

    private static String fullName(String prenom, String nom) {
        return FideleNameUtils.format(prenom, nom);
    }

    private static String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
