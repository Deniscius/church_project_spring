package com.eyram.dev.church_project_spring.utils.pdf;

import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.util.StringUtils;

import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Helpers de mise en page PDF (reçus / feuille d'intentions).
 */
public final class PdfDocumentStyles {

    public static final String ARCHIDIOCESE_LABEL = "ARCHIDIOCESE DE LOME";

    public static final Color HEADER_BG = new Color(31, 42, 55);
    public static final Color ACCENT = new Color(92, 46, 46);
    public static final Color SECTION_BG = new Color(245, 241, 235);
    public static final Color INTENTION_BG = new Color(252, 248, 240);
    public static final Color BORDER = new Color(208, 213, 221);
    public static final Color MUTED = new Color(102, 112, 133);
    public static final Color WHITE = Color.WHITE;

    private PdfDocumentStyles() {
    }

    public static Font titleFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, WHITE);
    }

    public static Font headingFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, ACCENT);
    }

    public static Font sectionFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, HEADER_BG);
    }

    public static Font labelFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, MUTED);
    }

    public static Font bodyFont() {
        return FontFactory.getFont(FontFactory.HELVETICA, 11, HEADER_BG);
    }

    public static Font intentionFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, HEADER_BG);
    }

    public static Font highlightLabelFont() {
        return FontFactory.getFont(FontFactory.HELVETICA, 9, MUTED);
    }

    public static Font highlightValueFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, HEADER_BG);
    }

    public static Font mutedFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, MUTED);
    }

    public static Font whiteSmallBold() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, WHITE);
    }

    public static Font stubRibbonFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, WHITE);
    }

    public static Font compactHeadingFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, ACCENT);
    }

    public static Font compactLabelFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, MUTED);
    }

    public static Font compactBodyFont() {
        return FontFactory.getFont(FontFactory.HELVETICA, 8, HEADER_BG);
    }

    public static Font compactIntentionFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, HEADER_BG);
    }

    public static Font cutLineFont() {
        return FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED);
    }

    /**
     * En-tête paroissial par défaut :
     * ARCHIDIOCESE DE LOME / nom / e-mail – contact, logo optionnel.
     */
    public static PdfPTable parishBrandedHeader(Paroisse paroisse, Path logoAbsolutePath, String documentTitle) {
        PdfPTable wrap = new PdfPTable(1);
        wrap.setWidthPercentage(100);
        wrap.setSpacingAfter(12);

        PdfPCell identity = new PdfPCell();
        identity.setBorderColor(BORDER);
        identity.setBorderWidth(1f);
        identity.setPadding(12);
        identity.setBackgroundColor(SECTION_BG);

        boolean hasLogo = logoAbsolutePath != null && Files.isRegularFile(logoAbsolutePath);
        try {
            if (hasLogo) {
                PdfPTable row = new PdfPTable(new float[]{1.2f, 4.8f});
                row.setWidthPercentage(100);

                Image logo = Image.getInstance(logoAbsolutePath.toAbsolutePath().toString());
                logo.scaleToFit(72, 72);
                PdfPCell logoCell = new PdfPCell(logo, false);
                logoCell.setBorder(0);
                logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                logoCell.setPaddingRight(10);
                row.addCell(logoCell);

                PdfPCell textCell = new PdfPCell();
                textCell.setBorder(0);
                textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                addIdentityLines(textCell, paroisse);
                row.addCell(textCell);

                identity.addElement(row);
            } else {
                addIdentityLines(identity, paroisse);
            }
        } catch (Exception ex) {
            identity = new PdfPCell();
            identity.setBorderColor(BORDER);
            identity.setBorderWidth(1f);
            identity.setPadding(12);
            identity.setBackgroundColor(SECTION_BG);
            addIdentityLines(identity, paroisse);
        }

        wrap.addCell(identity);

        if (StringUtils.hasText(documentTitle)) {
            PdfPCell titleCell = new PdfPCell();
            titleCell.setBackgroundColor(HEADER_BG);
            titleCell.setBorderColor(HEADER_BG);
            titleCell.setPadding(10);
            titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            Paragraph t = new Paragraph(documentTitle, titleFont());
            t.setAlignment(Element.ALIGN_CENTER);
            titleCell.addElement(t);
            wrap.addCell(titleCell);
        }

        return wrap;
    }

    private static void addIdentityLines(PdfPCell cell, Paroisse paroisse) {
        Paragraph org = new Paragraph(
                ARCHIDIOCESE_LABEL,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, HEADER_BG)
        );
        org.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(org);

        String parishName = paroisse != null && StringUtils.hasText(paroisse.getNom())
                ? paroisse.getNom()
                : "Paroisse";
        Paragraph name = new Paragraph(
                parishName,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, ACCENT)
        );
        name.setAlignment(Element.ALIGN_CENTER);
        name.setSpacingBefore(3);
        cell.addElement(name);

        String contact = contactLine(paroisse);
        if (StringUtils.hasText(contact)) {
            Paragraph detail = new Paragraph(
                    contact,
                    FontFactory.getFont(FontFactory.HELVETICA, 9, MUTED)
            );
            detail.setAlignment(Element.ALIGN_CENTER);
            detail.setSpacingBefore(3);
            cell.addElement(detail);
        }
    }

    public static String contactLine(Paroisse paroisse) {
        if (paroisse == null) {
            return null;
        }
        String email = StringUtils.hasText(paroisse.getEmail()) ? paroisse.getEmail().trim() : null;
        String phone = StringUtils.hasText(paroisse.getTelephone()) ? paroisse.getTelephone().trim() : null;
        if (email != null && phone != null) {
            return email + " – " + phone;
        }
        return email != null ? email : phone;
    }

    public static PdfPTable headerBanner(String title, String subtitle, String detail) {
        PdfPTable banner = new PdfPTable(1);
        banner.setWidthPercentage(100);
        banner.setSpacingAfter(14);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(HEADER_BG);
        cell.setBorderColor(HEADER_BG);
        cell.setPadding(14);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph t = new Paragraph(title, titleFont());
        t.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(t);

        if (subtitle != null && !subtitle.isBlank()) {
            Paragraph s = new Paragraph(subtitle, FontFactory.getFont(FontFactory.HELVETICA, 11, WHITE));
            s.setAlignment(Element.ALIGN_CENTER);
            s.setSpacingBefore(4);
            cell.addElement(s);
        }
        if (detail != null && !detail.isBlank()) {
            Paragraph d = new Paragraph(detail, FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(208, 213, 221)));
            d.setAlignment(Element.ALIGN_CENTER);
            d.setSpacingBefore(3);
            cell.addElement(d);
        }
        banner.addCell(cell);
        return banner;
    }

    public static Paragraph sectionTitle(String text) {
        Paragraph p = new Paragraph(text, headingFont());
        p.setSpacingBefore(10);
        p.setSpacingAfter(5);
        return p;
    }

    /** Case mise en avant (code de suivi, montant). */
    public static PdfPCell highlightCell(String label, String value) {
        PdfPTable inner = new PdfPTable(1);
        inner.setWidthPercentage(100);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, highlightLabelFont()));
        labelCell.setBorder(0);
        labelCell.setPadding(0);
        labelCell.setPaddingBottom(3);
        inner.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(
                value != null && !value.isBlank() ? value : "—",
                highlightValueFont()
        ));
        valueCell.setBorder(0);
        valueCell.setPadding(0);
        inner.addCell(valueCell);

        PdfPCell wrap = new PdfPCell(inner);
        wrap.setBackgroundColor(SECTION_BG);
        wrap.setBorderColor(BORDER);
        wrap.setBorderWidth(1f);
        wrap.setPadding(10);
        wrap.setPaddingLeft(12);
        wrap.setPaddingRight(12);
        return wrap;
    }

    public static PdfPCell sectionLabelCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, whiteSmallBold()));
        cell.setBackgroundColor(ACCENT);
        cell.setBorderColor(ACCENT);
        cell.setPadding(7);
        cell.setColspan(2);
        return cell;
    }

    public static void addMetaRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont()));
        PdfPCell valueCell = new PdfPCell(new Phrase(
                value != null && !value.isBlank() ? value : "—",
                bodyFont()
        ));
        styleMetaCell(labelCell);
        styleMetaCell(valueCell);
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    public static void styleMetaCell(PdfPCell cell) {
        cell.setBorderColor(BORDER);
        cell.setPadding(6);
        cell.setPaddingTop(5);
        cell.setPaddingBottom(5);
    }

    public static PdfPCell intentionCell(String intentionText) {
        PdfPTable inner = new PdfPTable(1);
        inner.setWidthPercentage(100);

        PdfPCell label = new PdfPCell(new Phrase("INTENTION DE MESSE", labelFont()));
        label.setBorder(0);
        label.setPadding(0);
        label.setPaddingBottom(4);
        inner.addCell(label);

        PdfPCell value = new PdfPCell(new Phrase(
                intentionText != null && !intentionText.isBlank() ? intentionText : "—",
                intentionFont()
        ));
        value.setBorder(0);
        value.setPadding(0);
        inner.addCell(value);

        PdfPCell wrap = new PdfPCell(inner);
        wrap.setBackgroundColor(INTENTION_BG);
        wrap.setBorderColor(BORDER);
        wrap.setPadding(10);
        wrap.setColspan(2);
        return wrap;
    }

    /** Bandeau indiquant le volet (fidèle / secrétariat). */
    public static PdfPTable stubRibbon(String label) {
        PdfPTable ribbon = new PdfPTable(1);
        ribbon.setWidthPercentage(100);
        ribbon.setSpacingAfter(6);

        PdfPCell cell = new PdfPCell(new Phrase(label, stubRibbonFont()));
        cell.setBackgroundColor(ACCENT);
        cell.setBorderColor(ACCENT);
        cell.setPadding(7);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        ribbon.addCell(cell);
        return ribbon;
    }

    /** Ligne de découpe entre les deux volets du reçu. */
    public static Paragraph tearLine() {
        Paragraph line = new Paragraph(
                "- - - - - - -  Decouper ici  - - - - - - -",
                cutLineFont()
        );
        line.setAlignment(Element.ALIGN_CENTER);
        line.setSpacingBefore(8);
        line.setSpacingAfter(8);
        return line;
    }

    public static Paragraph compactSectionTitle(String text) {
        Paragraph p = new Paragraph(text, compactHeadingFont());
        p.setSpacingBefore(6);
        p.setSpacingAfter(3);
        return p;
    }

    public static void addCompactMetaRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, compactLabelFont()));
        PdfPCell valueCell = new PdfPCell(new Phrase(
                value != null && !value.isBlank() ? value : "—",
                compactBodyFont()
        ));
        styleCompactMetaCell(labelCell);
        styleCompactMetaCell(valueCell);
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    public static void styleCompactMetaCell(PdfPCell cell) {
        cell.setBorderColor(BORDER);
        cell.setPadding(4);
        cell.setPaddingTop(3);
        cell.setPaddingBottom(3);
    }

    public static PdfPCell compactIntentionCell(String intentionText) {
        PdfPTable inner = new PdfPTable(1);
        inner.setWidthPercentage(100);

        PdfPCell label = new PdfPCell(new Phrase("INTENTION DE MESSE", compactLabelFont()));
        label.setBorder(0);
        label.setPadding(0);
        label.setPaddingBottom(2);
        inner.addCell(label);

        PdfPCell value = new PdfPCell(new Phrase(
                intentionText != null && !intentionText.isBlank() ? intentionText : "—",
                compactIntentionFont()
        ));
        value.setBorder(0);
        value.setPadding(0);
        inner.addCell(value);

        PdfPCell wrap = new PdfPCell(inner);
        wrap.setBackgroundColor(INTENTION_BG);
        wrap.setBorderColor(BORDER);
        wrap.setPadding(7);
        wrap.setColspan(2);
        return wrap;
    }

    /**
     * En-tête compact pour un demi-reçu (logo plus petit, titre court).
     */
    public static PdfPTable parishBrandedHeaderCompact(
            Paroisse paroisse,
            Path logoAbsolutePath,
            String documentTitle
    ) {
        PdfPTable wrap = new PdfPTable(1);
        wrap.setWidthPercentage(100);
        wrap.setSpacingAfter(3);

        PdfPCell identity = new PdfPCell();
        identity.setBorderColor(BORDER);
        identity.setBorderWidth(1f);
        identity.setPadding(8);
        identity.setBackgroundColor(SECTION_BG);

        boolean hasLogo = logoAbsolutePath != null && Files.isRegularFile(logoAbsolutePath);
        try {
            if (hasLogo) {
                PdfPTable row = new PdfPTable(new float[]{1f, 5.2f});
                row.setWidthPercentage(100);

                Image logo = Image.getInstance(logoAbsolutePath.toAbsolutePath().toString());
                logo.scaleToFit(34, 34);
                PdfPCell logoCell = new PdfPCell(logo, false);
                logoCell.setBorder(0);
                logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                logoCell.setPaddingRight(6);
                row.addCell(logoCell);

                PdfPCell textCell = new PdfPCell();
                textCell.setBorder(0);
                textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                addCompactIdentityLines(textCell, paroisse);
                row.addCell(textCell);

                identity.addElement(row);
            } else {
                addCompactIdentityLines(identity, paroisse);
            }
        } catch (Exception ex) {
            identity = new PdfPCell();
            identity.setBorderColor(BORDER);
            identity.setBorderWidth(1f);
            identity.setPadding(8);
            identity.setBackgroundColor(SECTION_BG);
            addCompactIdentityLines(identity, paroisse);
        }

        wrap.addCell(identity);

        if (StringUtils.hasText(documentTitle)) {
            PdfPCell titleCell = new PdfPCell();
            titleCell.setBackgroundColor(HEADER_BG);
            titleCell.setBorderColor(HEADER_BG);
            titleCell.setPadding(6);
            titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            Paragraph t = new Paragraph(
                    documentTitle,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, WHITE)
            );
            t.setAlignment(Element.ALIGN_CENTER);
            titleCell.addElement(t);
            wrap.addCell(titleCell);
        }

        return wrap;
    }

    private static void addCompactIdentityLines(PdfPCell cell, Paroisse paroisse) {
        Paragraph org = new Paragraph(
                ARCHIDIOCESE_LABEL,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, HEADER_BG)
        );
        org.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(org);

        String parishName = paroisse != null && StringUtils.hasText(paroisse.getNom())
                ? paroisse.getNom()
                : "Paroisse";
        Paragraph name = new Paragraph(
                parishName,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, ACCENT)
        );
        name.setAlignment(Element.ALIGN_CENTER);
        name.setSpacingBefore(2);
        cell.addElement(name);

        String contact = contactLine(paroisse);
        if (StringUtils.hasText(contact)) {
            Paragraph detail = new Paragraph(
                    contact,
                    FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED)
            );
            detail.setAlignment(Element.ALIGN_CENTER);
            detail.setSpacingBefore(2);
            cell.addElement(detail);
        }
    }
}
