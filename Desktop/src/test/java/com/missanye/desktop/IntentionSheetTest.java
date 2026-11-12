package com.missanye.desktop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.missanye.desktop.service.IntentionSheet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IntentionSheetTest {
    @Test void completeIntentionAndAllDatesArePrintableWithDefaultFaithfulName()throws Exception{
        var json=new ObjectMapper();var request=json.createObjectNode().put("localReference","MSL-731C7AB4-2FAE-4D78-A17B-0FF7653F4501").put("intention","Action de grâce pour la famille. ".repeat(12)+"DERNIÈRE PHRASE À IMPRIMER").put("telFidele","+22890123456").putNull("nomFidele").putNull("prenomFidele").put("horairePublicId","schedule");
        var dates=request.putArray("datesCelebration");for(int i=1;i<=30;i++)dates.add(String.format("2027-01-%02d",i));var catalogue=json.createObjectNode();catalogue.putArray("horaires").addObject().put("publicId","schedule").put("heureCelebration","08:30:00");
        byte[] pdf=IntentionSheet.render("Saint Paul",request,catalogue);try(var reader=new PdfReader(pdf)){
            StringBuilder text=new StringBuilder();for(int page=1;page<=reader.getNumberOfPages();page++)text.append(new PdfTextExtractor(reader).getTextFromPage(page));assertTrue(text.toString().contains("Un(e) chrétien(ne)"));assertTrue(text.toString().contains("DERNIÈRE PHRASE À IMPRIMER"));assertTrue(text.toString().contains("30/01/2027"));assertTrue(text.toString().contains("08 h 30"));assertFalse(text.toString().contains("null"));
        }
        java.nio.file.Files.write(java.nio.file.Path.of("target","desktop-intention-preview.pdf"),pdf);
    }
}
