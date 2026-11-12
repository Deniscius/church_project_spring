package com.missanye.desktop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.missanye.desktop.model.ScreenFormats;
import java.awt.Color;
import java.io.ByteArrayOutputStream;

/** Printable parish intention sheet; all intention text and celebration dates are retained. */
public final class IntentionSheet {
    private IntentionSheet(){}
    public static byte[] render(String parish,JsonNode demand,JsonNode catalogue)throws Exception{
        var bytes=new ByteArrayOutputStream();var document=new Document(PageSize.A4,40,40,36,36);PdfWriter.getInstance(document,bytes);document.addTitle("Fiche d'intention "+ScreenFormats.reference(demand));document.addAuthor(parish);document.open();
        var regular=LocalReceipt.font("DejaVuSans.ttf");var heavy=LocalReceipt.font("DejaVuSans-Bold.ttf");var font=new Font(regular,11);var bold=new Font(heavy,11);var title=new Font(heavy,18,Font.NORMAL,new Color(31,42,55));
        try{
            document.add(new Paragraph(parish,title));document.add(new Paragraph("FICHE D’INTENTION DE MESSE",bold));document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Code de suivi : "+ScreenFormats.reference(demand),bold));document.add(new Paragraph("Fidèle : "+ScreenFormats.faithful(demand),font));document.add(new Paragraph("Téléphone : "+demand.path("telFidele").asText("—"),font));
            document.add(Chunk.NEWLINE);document.add(new Paragraph("Intention",bold));var intention=new Paragraph(demand.path("intention").asText(""),font);intention.setLeading(17);document.add(intention);document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Célébrations",bold));var dates=demand.path("datesCelebration");
            if(dates.isEmpty())document.add(new Paragraph(ScreenFormats.date(demand.path("dateDebut").asText("")),font));
            for(var date:dates){JsonNode slot=demand;for(var s:demand.path("celebrationSlots"))if(date.asText("").equals(s.path("date").asText(""))){slot=s;break;}
                String time=slot.path("heurePersonnalisee").asText(slot.path("heureCelebration").asText(demand.path("horaireHeure").asText("")));
                if(time.isBlank())for(var h:catalogue.path("horaires"))if(slot.path("horairePublicId").asText("").equals(h.path("publicId").asText("")))time=h.path("heureCelebration").asText("");
                if(!time.isBlank())try{time=java.time.LocalTime.parse(time).format(java.time.format.DateTimeFormatter.ofPattern("HH' h 'mm"));}catch(Exception ignored){}
                document.add(new Paragraph(ScreenFormats.date(date.asText(""))+(time.isBlank()?"":" · "+time),font));
            }
        }finally{document.close();}return bytes.toByteArray();
    }
}
