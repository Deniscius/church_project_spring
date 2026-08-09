package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.entities.DemandeDate;
import com.eyram.dev.church_project_spring.entities.Facture;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.enums.ModePaiement;
import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;
import com.eyram.dev.church_project_spring.repositories.DemandeDateRepository;
import com.eyram.dev.church_project_spring.repositories.DemandeRepository;
import com.eyram.dev.church_project_spring.repositories.FactureRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.service.storage.StoredFileService;
import com.eyram.dev.church_project_spring.utils.BusinessCodeGenerator;
import com.eyram.dev.church_project_spring.utils.FideleNameUtils;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import com.eyram.dev.church_project_spring.utils.exception.TrackingIdNotFoundException;
import com.eyram.dev.church_project_spring.utils.pdf.PdfDocumentStyles;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Reçu PDF : une seule page A4 = ORIGINAL (haut) + découpe + DUPLICATA (bas).
 * Hauteurs fixes pour empêcher tout débordement sur une 2ᵉ page.
 */
@Service
@RequiredArgsConstructor
public class DemandeReceiptService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH' h 'mm");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH' h 'mm");

    /** Marges serrées pour tenir sur une page A4 (2 volets). */
    private static final float MARGIN_X = 20f;
    private static final float MARGIN_Y = 12f;
    private static final float CUT_HEIGHT = 20f;

    private final DemandeRepository demandeRepository;
    private final DemandeDateRepository demandeDateRepository;
    private final FactureRepository factureRepository;
    private final ParoisseRepository paroisseRepository;
    private final TenantAccessService tenantAccessService;
    private final StoredFileService storedFileService;

    @Transactional(readOnly = true)
    public byte[] generate(String trackingCode) {
        String code = BusinessCodeGenerator.normalizeDemandeTrackingCode(trackingCode);
        Demande demande = demandeRepository.findByCodeSuivieWithAssociations(code)
                .or(() -> demandeRepository.findByCodeSuivieWithAssociations(
                        trackingCode == null ? "" : trackingCode.trim()))
                .orElseThrow(() -> new TrackingIdNotFoundException("Code de suivi introuvable"));
        List<DemandeDate> dates = demandeDateRepository
                .findByDemandeAndStatusDelFalseOrderByOrdreAsc(demande);
        Facture facture = factureRepository
                .findByDemandePublicIdAndStatusDelFalse(demande.getPublicId())
                .orElse(null);

        ReceiptData data = ReceiptData.fromDemande(demande, dates, facture);
        return renderSingleA4(data, "Reçu " + demande.getCodeSuivie());
    }

    @Transactional(readOnly = true)
    public byte[] generateSampleForParoisse(UUID paroissePublicId) {
        Paroisse paroisse = paroisseRepository.findByPublicIdAndStatusDelFalse(paroissePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paroisse introuvable"));
        tenantAccessService.checkParoisseAccess(paroisse);

        ReceiptData data = new ReceiptData(
                paroisse,
                "MS-EXEMPLE-ABC123",
                "5 000 FCFA",
                LocalDateTime.now().format(DATE_TIME_FORMAT),
                "Exemple — Action de grâce pour la famille.",
                LocalDate.now().plusDays(3).format(DATE_FORMAT) + " · 07 h 00",
                "Messe (aperçu)",
                "Jean Dupont · +22890000000",
                "Espèces · Aperçu",
                null,
                "Conservez ce reçu. Code de suivi pour le suivi."
        );
        return renderSingleA4(data, "Aperçu reçu — " + paroisse.getNom());
    }

    private byte[] renderSingleA4(ReceiptData data, String title) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, MARGIN_X, MARGIN_X, MARGIN_Y, MARGIN_Y);
            PdfWriter writer = PdfWriter.getInstance(document, output);
            document.addTitle(title);
            document.open();

            float usableWidth = PageSize.A4.getWidth() - (MARGIN_X * 2f);
            float usableHeight = PageSize.A4.getHeight() - (MARGIN_Y * 2f);
            float halfHeight = (usableHeight - CUT_HEIGHT) / 2f;

            float pageHeight = PageSize.A4.getHeight();
            addWatermark(writer, "ORIGINAL", pageHeight * 0.72f);
            addWatermark(writer, "DUPLICATA", pageHeight * 0.28f);

            PdfPTable page = new PdfPTable(1);
            page.setTotalWidth(usableWidth);
            page.setLockedWidth(true);
            page.setSplitLate(false);
            page.setSplitRows(false);

            PdfPCell originalCell = new PdfPCell();
            originalCell.setBorder(0);
            originalCell.setPadding(0);
            originalCell.setFixedHeight(halfHeight);
            originalCell.addElement(buildReceiptBlock(data, "ORIGINAL", usableWidth));
            page.addCell(originalCell);

            PdfPCell cutCell = new PdfPCell();
            cutCell.setBorder(0);
            cutCell.setPadding(0);
            cutCell.setFixedHeight(CUT_HEIGHT);
            cutCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cutCell.addElement(buildCutLine(usableWidth));
            page.addCell(cutCell);

            PdfPCell duplicataCell = new PdfPCell();
            duplicataCell.setBorder(0);
            duplicataCell.setPadding(0);
            duplicataCell.setFixedHeight(halfHeight);
            duplicataCell.addElement(buildReceiptBlock(data, "DUPLICATA", usableWidth));
            page.addCell(duplicataCell);

            document.add(page);
            document.close();
            return output.toByteArray();
        } catch (DocumentException | java.io.IOException exception) {
            throw new IllegalStateException("Impossible de générer le reçu PDF", exception);
        }
    }

    private PdfPTable buildReceiptBlock(ReceiptData data, String copyLabel, float width)
            throws DocumentException {
        Path logoPath = resolveLogo(data.paroisse());

        PdfPTable block = new PdfPTable(1);
        block.setTotalWidth(width);
        block.setLockedWidth(true);
        block.setWidthPercentage(100);
        block.setSpacingBefore(0);
        block.setSpacingAfter(0);

        // Badge ORIGINAL / DUPLICATA
        PdfPTable badgeRow = new PdfPTable(1);
        badgeRow.setWidthPercentage(100);
        PdfPCell badge = new PdfPCell(new Phrase(
                copyLabel,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(90, 90, 90))
        ));
        badge.setBorder(0);
        badge.setHorizontalAlignment(Element.ALIGN_RIGHT);
        badge.setPadding(0);
        badge.setPaddingBottom(2);
        badgeRow.addCell(badge);
        block.addCell(wrapNoBorder(badgeRow));

        // En-tête compact
        PdfPTable header = PdfDocumentStyles.parishBrandedHeaderCompact(
                data.paroisse(),
                logoPath,
                "REÇU DE DÉPÔT"
        );
        header.setSpacingAfter(4);
        block.addCell(wrapNoBorder(header));

        // Code / montant / dépôt
        PdfPTable highlight = new PdfPTable(new float[]{1.2f, 1f, 1.15f});
        highlight.setWidthPercentage(100);
        highlight.setSpacingAfter(4);
        highlight.addCell(compactHighlight("Code de suivi", data.codeSuivie()));
        highlight.addCell(compactHighlight("Montant", data.montant()));
        highlight.addCell(compactHighlight("Dépôt", data.depot()));
        block.addCell(wrapNoBorder(highlight));

        // Intention
        PdfPTable intention = new PdfPTable(1);
        intention.setWidthPercentage(100);
        intention.setSpacingAfter(3);
        intention.addCell(PdfDocumentStyles.compactIntentionCell(truncate(data.intention(), 160)));
        block.addCell(wrapNoBorder(intention));

        // Méta
        PdfPTable meta = new PdfPTable(new float[]{1.2f, 3.3f});
        meta.setWidthPercentage(100);
        meta.setSpacingAfter(2);
        PdfDocumentStyles.addCompactMetaRow(meta, "Célébration", truncate(data.celebration(), 120));
        PdfDocumentStyles.addCompactMetaRow(meta, "Type", truncate(data.type(), 80));
        PdfDocumentStyles.addCompactMetaRow(meta, "Demandeur", truncate(data.demandeur(), 90));
        PdfDocumentStyles.addCompactMetaRow(meta, "Paiement", truncate(data.paiement(), 90));
        if (StringUtils.hasText(data.refFacture())) {
            PdfDocumentStyles.addCompactMetaRow(meta, "Réf. facture", data.refFacture());
        }
        block.addCell(wrapNoBorder(meta));

        PdfPCell notice = new PdfPCell(new Phrase(
                data.notice() != null ? data.notice() : "",
                FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(102, 112, 133))
        ));
        notice.setBorder(0);
        notice.setPadding(0);
        notice.setPaddingTop(2);
        block.addCell(notice);

        return block;
    }

    private static PdfPTable buildCutLine(float width) throws DocumentException {
        PdfPTable cut = new PdfPTable(new float[]{0.5f, 4.5f, 0.5f});
        cut.setTotalWidth(width);
        cut.setLockedWidth(true);
        cut.setWidthPercentage(100);

        PdfPCell left = new PdfPCell(new Phrase("✂", PdfDocumentStyles.cutLineFont()));
        left.setBorder(0);
        left.setHorizontalAlignment(Element.ALIGN_CENTER);
        left.setVerticalAlignment(Element.ALIGN_MIDDLE);
        left.setPadding(0);

        PdfPCell middle = new PdfPCell(new Phrase(
                "— — —  Découper ici  — — —",
                PdfDocumentStyles.cutLineFont()
        ));
        middle.setBorder(0);
        middle.setBorderWidthTop(0.7f);
        middle.setBorderColorTop(new Color(120, 120, 120));
        middle.setPaddingTop(4);
        middle.setPaddingBottom(0);
        middle.setHorizontalAlignment(Element.ALIGN_CENTER);
        middle.setVerticalAlignment(Element.ALIGN_MIDDLE);

        PdfPCell right = new PdfPCell(new Phrase("✂", PdfDocumentStyles.cutLineFont()));
        right.setBorder(0);
        right.setHorizontalAlignment(Element.ALIGN_CENTER);
        right.setVerticalAlignment(Element.ALIGN_MIDDLE);
        right.setPadding(0);

        cut.addCell(left);
        cut.addCell(middle);
        cut.addCell(right);
        return cut;
    }

    private static PdfPCell wrapNoBorder(PdfPTable table) {
        PdfPCell cell = new PdfPCell(table);
        cell.setBorder(0);
        cell.setPadding(0);
        return cell;
    }

    private static PdfPCell compactHighlight(String label, String value) {
        PdfPTable inner = new PdfPTable(1);
        inner.setWidthPercentage(100);

        PdfPCell labelCell = new PdfPCell(new Phrase(
                label,
                FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(102, 112, 133))
        ));
        labelCell.setBorder(0);
        labelCell.setPadding(0);
        labelCell.setPaddingBottom(2);
        inner.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(
                value != null && !value.isBlank() ? value : "—",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(16, 24, 40))
        ));
        valueCell.setBorder(0);
        valueCell.setPadding(0);
        inner.addCell(valueCell);

        PdfPCell wrap = new PdfPCell(inner);
        wrap.setBackgroundColor(new Color(248, 250, 252));
        wrap.setBorderColor(new Color(208, 213, 221));
        wrap.setBorderWidth(0.8f);
        wrap.setPadding(6);
        wrap.setPaddingLeft(7);
        wrap.setPaddingRight(7);
        return wrap;
    }

    private static String truncate(String value, int max) {
        if (value == null || value.isBlank()) {
            return "—";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, Math.max(0, max - 1)).trim() + "…";
    }

    private void addWatermark(PdfWriter writer, String text, float yCenter) {
        try {
            PdfContentByte canvas = writer.getDirectContentUnder();
            PdfGState gs = new PdfGState();
            gs.setFillOpacity(0.08f);
            canvas.saveState();
            canvas.setGState(gs);
            canvas.setColorFill(new Color(90, 90, 90));
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            canvas.beginText();
            canvas.setFontAndSize(bf, 36);
            canvas.showTextAligned(
                    Element.ALIGN_CENTER,
                    text,
                    PageSize.A4.getWidth() / 2f,
                    yCenter,
                    28
            );
            canvas.endText();
            canvas.restoreState();
        } catch (Exception ignored) {
            // Filigrane optionnel.
        }
    }

    private Path resolveLogo(Paroisse paroisse) {
        if (paroisse == null || !StringUtils.hasText(paroisse.getLogoPath())) {
            return null;
        }
        try {
            return storedFileService.resolveAbsolute(paroisse.getLogoPath());
        } catch (Exception ignored) {
            return null;
        }
    }

    private record ReceiptData(
            Paroisse paroisse,
            String codeSuivie,
            String montant,
            String depot,
            String intention,
            String celebration,
            String type,
            String demandeur,
            String paiement,
            String refFacture,
            String notice
    ) {
        static ReceiptData fromDemande(Demande demande, List<DemandeDate> dates, Facture facture) {
            String depot = demande.getCreatedAt() != null
                    ? demande.getCreatedAt().format(DATE_TIME_FORMAT)
                    : "—";
            BigDecimal montant = demande.getMontant() != null
                    ? demande.getMontant().setScale(0, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            String ref = facture != null && StringUtils.hasText(facture.getRefFacture())
                    ? facture.getRefFacture()
                    : null;
            return new ReceiptData(
                    demande.getParoisse(),
                    demande.getCodeSuivie(),
                    montant + " FCFA",
                    depot,
                    demande.getIntention(),
                    formatCelebrationDates(dates) + " · " + celebrationTime(demande),
                    demande.getTypeDemande() != null ? demande.getTypeDemande().getLibelle() : "—",
                    FideleNameUtils.format(demande.getPrenomFidele(), demande.getNomFidele())
                            + (StringUtils.hasText(demande.getTelFidele())
                            ? " · " + demande.getTelFidele() : ""),
                    paymentModeLabel(demande) + " · " + labelStatutPaiement(demande.getStatutPaiement()),
                    ref,
                    "Conservez ce reçu. Code de suivi pour le suivi."
            );
        }
    }

    private static String formatCelebrationDates(List<DemandeDate> dates) {
        if (dates == null || dates.isEmpty()) {
            return "—";
        }
        // Limite l’affichage pour rester sur une page (neuvaine / trentaine).
        int limit = Math.min(dates.size(), 4);
        String joined = dates.stream()
                .limit(limit)
                .map(item -> item.getDateCelebration().format(DATE_FORMAT))
                .collect(Collectors.joining(" · "));
        if (dates.size() > limit) {
            joined = joined + " · +" + (dates.size() - limit);
        }
        return joined;
    }

    private static String celebrationTime(Demande demande) {
        if (demande.getHeurePersonnalisee() != null) {
            return demande.getHeurePersonnalisee().format(TIME_FORMAT);
        }
        return demande.getHoraire() != null && demande.getHoraire().getHeureCelebration() != null
                ? demande.getHoraire().getHeureCelebration().format(TIME_FORMAT)
                : "—";
    }

    private static String paymentModeLabel(Demande demande) {
        if (demande.getTypePaiement() == null) {
            return "—";
        }
        if (demande.getTypePaiement().getMode() == ModePaiement.ESPECES) {
            String parish = demande.getParoisse() != null ? demande.getParoisse().getNom() : "la paroisse";
            return "Espèces · " + parish;
        }
        return demande.getTypePaiement().getLibelle();
    }

    private static String labelStatutPaiement(StatutPaiementEnum value) {
        if (value == null) {
            return "—";
        }
        return switch (value) {
            case PAYE -> "Payée";
            case EN_ATTENTE -> "En attente";
            case NON_PAYE -> "Non payée";
            case ECHOUE -> "Échouée";
        };
    }
}
