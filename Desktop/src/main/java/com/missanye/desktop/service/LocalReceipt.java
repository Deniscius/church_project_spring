package com.missanye.desktop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.missanye.desktop.model.ScreenFormats;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;

/** Local document; never represents a provider payment or a central confirmation that has not occurred. */
public final class LocalReceipt {
    private LocalReceipt(){}
    public static byte[] render(String parish,JsonNode demand)throws Exception{
        var bytes=new ByteArrayOutputStream();var document=new Document(PageSize.A4,42,42,42,42);PdfWriter.getInstance(document,bytes);document.addTitle("Reçu "+demand.path("localReference").asText());document.addAuthor(parish);document.open();
        try{
            var blue=new Color(36,83,151);var regular=font("DejaVuSans.ttf");var heavy=font("DejaVuSans-Bold.ttf");var title=new Font(heavy,20,Font.NORMAL,blue);var normal=new Font(regular,11);var bold=new Font(heavy,11);
            document.add(new Paragraph(parish,title));document.add(new Paragraph("MISSANYE — REÇU PAROISSIAL",bold));document.add(Chunk.NEWLINE);
            boolean received=demand.path("localCashReceived").asBoolean(),pending=demand.path("localPending").asBoolean(),review=demand.path("localReconciliationRequired").asBoolean();
            String status=review?"À RAPPROCHER AVEC LE SERVICE":pending?"DOCUMENT LOCAL — TRANSMISSION EN ATTENTE":"COPIE LOCALE DES INFORMATIONS DU SERVICE";
            var banner=new PdfPTable(1);banner.setWidthPercentage(100);var cell=new PdfPCell(new Phrase(status,bold));cell.setPadding(12);cell.setBackgroundColor(review?new Color(255,230,224):new Color(237,243,252));banner.addCell(cell);document.add(banner);document.add(Chunk.NEWLINE);
            var table=new PdfPTable(new float[]{1.1f,2.5f});table.setWidthPercentage(100);
            row(table,"Référence de suivi",demand.path("localReference").asText(),normal);
            row(table,"Document",(received?"LRC-"+demand.path("localCashId").asText():"LREQ-"+demand.path("localRequestId").asText()),normal);
            if(!demand.path("publicId").asText().startsWith("local:"))row(table,"Code du service",demand.path("codeSuivie").asText(),normal);
            row(table,"Fidèle",ScreenFormats.faithful(demand),normal);row(table,"Téléphone",demand.path("telFidele").asText(),normal);
            row(table,"Montant relevé",ScreenFormats.money(demand.path("localQuotedMontant")),normal);
            row(table,received?"Espèces":"Paiement",received?"Somme reçue sur le poste":"PAYE".equals(demand.path("statutPaiement").asText())?"Confirmé dans les dernières informations reçues du service":"Aucun encaissement attesté par ce document",normal);
            row(table,"Décision",ScreenFormats.state(demand.path("localValidation").asText(demand.path("statutValidation").asText())),normal);
            if(received)row(table,"Encaissement enregistré",date(demand.path("localCashRecordedAt").asText(demand.path("localRecordedAt").asText())),normal);row(table,"Enregistrement",date(demand.path("localRecordedAt").asText()),normal);document.add(table);document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Intention",bold));document.add(new Paragraph(demand.path("intention").asText(),normal));document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Dates de célébration",bold));if(demand.path("datesCelebration").isArray())for(var date:demand.path("datesCelebration"))document.add(new Paragraph(ScreenFormats.date(date.asText()),normal));else document.add(new Paragraph(ScreenFormats.date(demand.path("dateDebut").asText()),normal));
            document.add(Chunk.NEWLINE);document.add(new Paragraph(pending?"La référence reste valable après transmission. Avant celle-ci, le suivi est disponible sur le poste d'enregistrement. Les disponibilités, tarifs et décisions seront contrôlés par le service.":"Conservez la référence de suivi. Le reçu officiel du service reste disponible auprès de la paroisse.",normal));
            if(review)document.add(new Paragraph("Un retour du service nécessite un rapprochement. Les espèces déjà reçues restent à traiter par la paroisse ; ce document ne les annule pas.",bold));
            document.add(Chunk.NEWLINE);document.add(new Paragraph("Édition : "+date(Instant.now().toString()),new Font(regular,8,Font.NORMAL,Color.GRAY)));
        }finally{document.close();}return bytes.toByteArray();
    }
    private static BaseFont font(String name)throws Exception{
        try(var stream=LocalReceipt.class.getResourceAsStream("/com/missanye/desktop/branding/fonts/"+name)){
            if(stream==null)throw new java.io.IOException("Police du reçu introuvable");return BaseFont.createFont(name,BaseFont.IDENTITY_H,BaseFont.EMBEDDED,true,stream.readAllBytes(),null);
        }
    }
    private static String date(String text){
        var format=java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu 'à' HH:mm").withZone(java.time.ZoneId.of("Africa/Lome"));
        try{return format.format(Instant.parse(text));}catch(Exception ignored){try{return java.time.LocalDateTime.parse(text).format(format);}catch(Exception invalid){return text;}}
    }
    private static void row(PdfPTable table,String label,String value,Font font){for(String text:new String[]{label,value}){var cell=new PdfPCell(new Phrase(text,font));cell.setPadding(8);cell.setBorderColor(new Color(220,228,238));table.addCell(cell);}}
}
