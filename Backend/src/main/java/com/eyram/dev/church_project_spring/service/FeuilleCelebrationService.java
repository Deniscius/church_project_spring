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
import com.eyram.dev.church_project_spring.utils.pdf.PdfDocumentStyles;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
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
        int totalIntentions = messes.stream()
                .mapToInt(CelebrationMesseGroupResponse::nombreIntentions)
                .sum();

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 40);
            PdfWriter.getInstance(document, output);
            document.addTitle("Feuille d'intentions — " + date.format(DATE_SHORT));
            document.open();

            java.nio.file.Path logoPath = null;
            if (org.springframework.util.StringUtils.hasText(paroisse.getLogoPath())) {
                try {
                    logoPath = storedFileService.resolveAbsolute(paroisse.getLogoPath());
                } catch (Exception ignored) {
                    logoPath = null;
                }
            }
            document.add(PdfDocumentStyles.parishBrandedHeader(
                    paroisse,
                    logoPath,
                    "FEUILLE D'INTENTIONS DE MESSES"
            ));

            PdfPTable celebrationBanner = new PdfPTable(1);
            celebrationBanner.setWidthPercentage(100);
            celebrationBanner.setSpacingAfter(12);
            PdfPCell celebrationCell = new PdfPCell();
            celebrationCell.setBackgroundColor(PdfDocumentStyles.SECTION_BG);
            celebrationCell.setBorderColor(PdfDocumentStyles.BORDER);
            celebrationCell.setPadding(10);
            celebrationCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            celebrationCell.addElement(new Paragraph("DATE DE CÉLÉBRATION", PdfDocumentStyles.labelFont()));
            Paragraph celebrationDate = new Paragraph(
                    capitalize(date.format(DATE_LONG)),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, PdfDocumentStyles.HEADER_BG)
            );
            celebrationDate.setAlignment(Element.ALIGN_CENTER);
            celebrationDate.setSpacingBefore(3);
            celebrationCell.addElement(celebrationDate);
            celebrationBanner.addCell(celebrationCell);
            document.add(celebrationBanner);

            String filterNote = inclureNonPayees
                    ? "Intentions validées (payées et non payées)."
                    : "Intentions validées et payées uniquement.";
            if (heures != null && !heures.isEmpty()) {
                filterNote += "  ·  Messes de " + formatHeures(heures) + ".";
            }
            Paragraph summary = new Paragraph(
                    filterNote + "  ·  " + totalIntentions + " intention" + (totalIntentions > 1 ? "s" : "")
                            + "  ·  " + messes.size() + " messe" + (messes.size() > 1 ? "s" : "") + ".",
                    PdfDocumentStyles.mutedFont()
            );
            summary.setAlignment(Element.ALIGN_CENTER);
            summary.setSpacingAfter(10);
            document.add(summary);

            if (messes.isEmpty()) {
                Paragraph empty = new Paragraph(
                        "Aucune intention à célébrer pour cette date de célébration.",
                        PdfDocumentStyles.bodyFont()
                );
                empty.setAlignment(Element.ALIGN_CENTER);
                document.add(empty);
            } else {
                Paragraph plan = new Paragraph(
                        "Plan des messes : "
                                + messes.stream()
                                .map(g -> g.libelleMesse() + " (" + g.nombreIntentions() + ")")
                                .reduce((a, b) -> a + "  ·  " + b)
                                .orElse("—"),
                        PdfDocumentStyles.mutedFont()
                );
                plan.setAlignment(Element.ALIGN_CENTER);
                plan.setSpacingAfter(12);
                document.add(plan);

                for (CelebrationMesseGroupResponse messe : messes) {
                    document.add(buildMesseBanner(messe));
                    int index = 1;
                    for (CelebrationIntentionResponse intention : messe.intentions()) {
                        document.add(buildIntentionCard(intention, index++));
                    }
                }
            }

            Paragraph footer = new Paragraph(
                    "Document destiné à la sacristie et au célébrant. "
                            + "Les intentions sont regroupées par heure de messe. "
                            + "La date ci-dessus est celle de la célébration (distincte de la date de dépôt).",
                    PdfDocumentStyles.mutedFont()
            );
            footer.setSpacingBefore(16);
            document.add(footer);

            document.close();
            return output.toByteArray();
        } catch (DocumentException | java.io.IOException exception) {
            throw new IllegalStateException("Impossible de générer la feuille d'intentions PDF", exception);
        }
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

    private PdfPTable buildMesseBanner(CelebrationMesseGroupResponse messe) {
        PdfPTable banner = new PdfPTable(1);
        banner.setWidthPercentage(100);
        banner.setSpacingBefore(12);
        banner.setSpacingAfter(8);

        String title = messe.libelleMesse().toUpperCase(Locale.FRENCH)
                + "  —  "
                + messe.nombreIntentions()
                + " intention"
                + (messe.nombreIntentions() > 1 ? "s" : "");

        PdfPCell cell = new PdfPCell(new Phrase(title, PdfDocumentStyles.whiteSmallBold()));
        cell.setBackgroundColor(PdfDocumentStyles.ACCENT);
        cell.setBorderColor(PdfDocumentStyles.ACCENT);
        cell.setPadding(10);
        banner.addCell(cell);
        return banner;
    }

    private PdfPTable buildIntentionCard(CelebrationIntentionResponse info, int indexInMesse)
            throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{1.35f, 3.15f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(10);

        String rankLabel = "Intention n° " + indexInMesse;
        if (info.heure() != null) {
            rankLabel += "  ·  " + info.heure().format(TIME_FORMAT);
        }
        PdfPCell rank = new PdfPCell(new Phrase(rankLabel, PdfDocumentStyles.whiteSmallBold()));
        rank.setBackgroundColor(PdfDocumentStyles.HEADER_BG);
        rank.setBorderColor(PdfDocumentStyles.HEADER_BG);
        rank.setPadding(7);
        rank.setColspan(2);
        table.addCell(rank);

        // Feuille légère pour le célébrant : intention, demandeur, progression (triduum/neuvaine).
        table.addCell(PdfDocumentStyles.intentionCell(info.intention()));
        PdfDocumentStyles.addMetaRow(table, "Demandeur", fullName(info.demandeurPrenom(), info.demandeurNom()));
        if (info.progressionLabel() != null && !info.progressionLabel().isBlank()) {
            PdfDocumentStyles.addMetaRow(table, "Progression", info.progressionLabel());
        }
        if (info.celebre()) {
            PdfDocumentStyles.addMetaRow(table, "Statut", "Célébrée");
        }
        return table;
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
