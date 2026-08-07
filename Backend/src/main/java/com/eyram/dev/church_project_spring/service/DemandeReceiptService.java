package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.entities.DemandeDate;
import com.eyram.dev.church_project_spring.entities.Facture;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.enums.ModePaiement;
import com.eyram.dev.church_project_spring.enums.StatutDemandeEnum;
import com.eyram.dev.church_project_spring.enums.StatutPaiementEnum;
import com.eyram.dev.church_project_spring.enums.StatutValidationEnum;
import com.eyram.dev.church_project_spring.repositories.DemandeDateRepository;
import com.eyram.dev.church_project_spring.repositories.DemandeRepository;
import com.eyram.dev.church_project_spring.repositories.FactureRepository;
import com.eyram.dev.church_project_spring.service.storage.StoredFileService;
import com.eyram.dev.church_project_spring.utils.BusinessCodeGenerator;
import com.eyram.dev.church_project_spring.utils.FideleNameUtils;
import com.eyram.dev.church_project_spring.utils.exception.TrackingIdNotFoundException;
import com.eyram.dev.church_project_spring.utils.pdf.PdfDocumentStyles;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reçu PDF sur une seule page A4 : infos essentielles, typographie lisible.
 */
@Service
@RequiredArgsConstructor
public class DemandeReceiptService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final DemandeRepository demandeRepository;
    private final DemandeDateRepository demandeDateRepository;
    private final FactureRepository factureRepository;
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

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 42, 42, 36, 36);
            PdfWriter.getInstance(document, output);
            document.addTitle("Reçu " + demande.getCodeSuivie());
            document.open();

            Paroisse paroisse = demande.getParoisse();
            Path logoPath = resolveLogo(paroisse);

            document.add(PdfDocumentStyles.parishBrandedHeader(
                    paroisse,
                    logoPath,
                    "REÇU DE DÉPÔT"
            ));

            // Code + montant mis en avant
            PdfPTable highlight = new PdfPTable(new float[]{1f, 1f});
            highlight.setWidthPercentage(100);
            highlight.setSpacingAfter(10);
            highlight.addCell(PdfDocumentStyles.highlightCell("Code de suivi", demande.getCodeSuivie()));
            highlight.addCell(PdfDocumentStyles.highlightCell(
                    "Montant",
                    demande.getMontant().setScale(0, RoundingMode.HALF_UP) + " FCFA"
            ));
            document.add(highlight);

            document.add(PdfDocumentStyles.sectionTitle("Intention"));
            PdfPTable intention = new PdfPTable(1);
            intention.setWidthPercentage(100);
            intention.setSpacingAfter(8);
            intention.addCell(PdfDocumentStyles.intentionCell(demande.getIntention()));
            document.add(intention);

            document.add(PdfDocumentStyles.sectionTitle("Célébration"));
            PdfPTable celebration = metaTable();
            PdfDocumentStyles.addMetaRow(celebration, "Date(s)", formatCelebrationDates(dates));
            PdfDocumentStyles.addMetaRow(celebration, "Heure", celebrationTime(demande));
            PdfDocumentStyles.addMetaRow(celebration, "Type",
                    demande.getTypeDemande() != null ? demande.getTypeDemande().getLibelle() : "—");
            document.add(celebration);

            document.add(PdfDocumentStyles.sectionTitle("Demandeur"));
            PdfPTable applicant = metaTable();
            PdfDocumentStyles.addMetaRow(applicant, "Nom",
                    FideleNameUtils.format(demande.getPrenomFidele(), demande.getNomFidele()));
            PdfDocumentStyles.addMetaRow(applicant, "Téléphone", demande.getTelFidele());
            if (StringUtils.hasText(demande.getEmailFidele())) {
                PdfDocumentStyles.addMetaRow(applicant, "E-mail", demande.getEmailFidele());
            }
            document.add(applicant);

            document.add(PdfDocumentStyles.sectionTitle("Paiement"));
            PdfPTable payment = metaTable();
            PdfDocumentStyles.addMetaRow(payment, "Mode", paymentModeLabel(demande));
            PdfDocumentStyles.addMetaRow(payment, "Statut", labelStatutPaiement(demande.getStatutPaiement()));
            PdfDocumentStyles.addMetaRow(payment, "Date de dépôt",
                    demande.getCreatedAt() != null ? demande.getCreatedAt().format(DATE_TIME_FORMAT) : "—");
            if (facture != null && StringUtils.hasText(facture.getRefFacture())) {
                PdfDocumentStyles.addMetaRow(payment, "Réf. facture", facture.getRefFacture());
            }
            document.add(payment);

            String statusLine = "Demande : " + labelStatutDemande(demande.getStatutDemande())
                    + "  ·  Validation : " + labelStatutValidation(demande.getStatutValidation());
            Paragraph statuses = new Paragraph(statusLine, PdfDocumentStyles.bodyFont());
            statuses.setSpacingBefore(10);
            statuses.setSpacingAfter(6);
            document.add(statuses);

            Paragraph notice = new Paragraph(
                    "Conservez ce reçu. Le code de suivi permet de suivre la demande. "
                            + "Preuve de paiement uniquement si le statut est « Payée ».",
                    PdfDocumentStyles.mutedFont()
            );
            notice.setSpacingBefore(8);
            document.add(notice);

            document.close();
            return output.toByteArray();
        } catch (DocumentException | java.io.IOException exception) {
            throw new IllegalStateException("Impossible de générer le reçu PDF", exception);
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

    private PdfPTable metaTable() throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{1.3f, 3.2f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(4);
        return table;
    }

    private String formatCelebrationDates(List<DemandeDate> dates) {
        if (dates == null || dates.isEmpty()) {
            return "—";
        }
        return dates.stream()
                .map(item -> item.getDateCelebration().format(DATE_FORMAT))
                .collect(Collectors.joining(" · "));
    }

    private String celebrationTime(Demande demande) {
        if (demande.getHeurePersonnalisee() != null) {
            return demande.getHeurePersonnalisee().toString();
        }
        return demande.getHoraire() != null && demande.getHoraire().getHeureCelebration() != null
                ? demande.getHoraire().getHeureCelebration().toString()
                : "—";
    }

    private String paymentModeLabel(Demande demande) {
        if (demande.getTypePaiement() == null) {
            return "—";
        }
        if (demande.getTypePaiement().getMode() == ModePaiement.ESPECES) {
            String parish = demande.getParoisse() != null ? demande.getParoisse().getNom() : "la paroisse";
            return "Au comptant à " + parish;
        }
        return demande.getTypePaiement().getLibelle();
    }

    private static String labelStatutDemande(StatutDemandeEnum value) {
        if (value == null) {
            return "—";
        }
        return switch (value) {
            case EN_ATTENTE -> "En attente";
            case VALIDEE -> "Validée";
            case REJETEE -> "Rejetée";
            case ANNULEE -> "Annulée";
            case TERMINEE -> "Terminée";
        };
    }

    private static String labelStatutValidation(StatutValidationEnum value) {
        if (value == null) {
            return "—";
        }
        return switch (value) {
            case EN_ATTENTE -> "En attente";
            case VALIDEE -> "Validée";
            case REJETEE -> "Rejetée";
        };
    }

    private static String labelStatutPaiement(StatutPaiementEnum value) {
        if (value == null) {
            return "—";
        }
        return switch (value) {
            case NON_PAYE -> "Non payée";
            case EN_ATTENTE -> "Paiement en cours";
            case PAYE -> "Payée";
            case ECHOUE -> "Échouée";
        };
    }
}
