package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.entities.DemandeDate;
import com.eyram.dev.church_project_spring.entities.Facture;
import com.eyram.dev.church_project_spring.enums.ModePaiement;
import com.eyram.dev.church_project_spring.repositories.DemandeDateRepository;
import com.eyram.dev.church_project_spring.repositories.DemandeRepository;
import com.eyram.dev.church_project_spring.repositories.FactureRepository;
import com.eyram.dev.church_project_spring.utils.exception.TrackingIdNotFoundException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
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
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DemandeReceiptService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final DemandeRepository demandeRepository;
    private final DemandeDateRepository demandeDateRepository;
    private final FactureRepository factureRepository;

    @Transactional(readOnly = true)
    public byte[] generate(String trackingCode) {
        Demande demande = demandeRepository.findByCodeSuivieAndStatusDelFalse(trackingCode)
                .orElseThrow(() -> new TrackingIdNotFoundException("Code de suivi introuvable"));
        List<DemandeDate> dates = demandeDateRepository
                .findByDemandeAndStatusDelFalseOrderByOrdreAsc(demande);
        Facture facture = factureRepository
                .findByDemandePublicIdAndStatusDelFalse(demande.getPublicId())
                .orElse(null);

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 48, 48, 48, 48);
            PdfWriter.getInstance(document, output);
            document.addTitle("Reçu de dépôt " + demande.getCodeSuivie());
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Paragraph title = new Paragraph("REÇU DE DÉPÔT DE DEMANDE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph parish = new Paragraph(demande.getParoisse().getNom(), subtitleFont);
            parish.setAlignment(Element.ALIGN_CENTER);
            parish.setSpacingAfter(22);
            document.add(parish);

            PdfPTable table = new PdfPTable(new float[]{1.4f, 2.6f});
            table.setWidthPercentage(100);
            addRow(table, "Code de suivi", demande.getCodeSuivie());
            addRow(table, "Référence facture", facture != null ? facture.getRefFacture() : "—");
            addRow(table, "Demandeur", demande.getPrenomFidele() + " " + demande.getNomFidele());
            addRow(table, "Téléphone", demande.getTelFidele());
            addRow(table, "Doyenné", demande.getParoisse().getDoyenne().getNom());
            addRow(table, "Paroisse", demande.getParoisse().getNom());
            addRow(table, "Type de demande", demande.getTypeDemande().getLibelle());
            addRow(table, "Intention", demande.getIntention());
            addRow(table, "Date(s)", dates.stream()
                    .map(item -> item.getDateCelebration().format(DATE_FORMAT))
                    .reduce((left, right) -> left + ", " + right).orElse("—"));
            addRow(table, "Horaire", celebrationTime(demande));
            addRow(table, "Montant", demande.getMontant().setScale(0, RoundingMode.HALF_UP) + " FCFA");
            addRow(table, "Paiement", paymentLabel(demande));
            addRow(table, "Statut", demande.getStatutDemande().name());
            addRow(table, "Déposée le", demande.getCreatedAt() != null
                    ? demande.getCreatedAt().format(DATE_TIME_FORMAT) : "—");
            document.add(table);

            Paragraph notice = new Paragraph(
                    "Ce document confirme l'enregistrement de la demande. Il ne constitue une preuve de paiement "
                            + "que lorsque le statut de paiement est PAYÉ.",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9)
            );
            notice.setSpacingBefore(20);
            document.add(notice);
            document.close();
            return output.toByteArray();
        } catch (DocumentException | java.io.IOException exception) {
            throw new IllegalStateException("Impossible de générer le reçu PDF", exception);
        }
    }

    private void addRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "—", FontFactory.getFont(FontFactory.HELVETICA, 10)));
        labelCell.setPadding(8);
        valueCell.setPadding(8);
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private String celebrationTime(Demande demande) {
        if (demande.getHeurePersonnalisee() != null) {
            return demande.getHeurePersonnalisee().toString();
        }
        return demande.getHoraire() != null && demande.getHoraire().getHeureCelebration() != null
                ? demande.getHoraire().getHeureCelebration().toString()
                : "—";
    }

    private String paymentLabel(Demande demande) {
        if (demande.getTypePaiement() == null) return "—";
        if (demande.getTypePaiement().getMode() == ModePaiement.ESPECES) {
            return "Au comptant à " + demande.getParoisse().getNom()
                    + " — " + demande.getStatutPaiement().name();
        }
        return demande.getTypePaiement().getLibelle() + " — " + demande.getStatutPaiement().name();
    }
}
