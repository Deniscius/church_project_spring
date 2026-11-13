package com.missanye.desktop;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.missanye.desktop.service.IntentionSheet;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class IntentionSheetTest {
    private final ObjectMapper json=new ObjectMapper();private final LocalDate day=LocalDate.parse("2027-01-11");
    private ObjectNode request(String text,String hour,String payment){var r=json.createObjectNode().put("publicId",text).put("intention",text).put("statutDemande","VALIDEE").put("statutValidation","VALIDEE").put("statutPaiement",payment).put("horaireHeure",hour).putNull("nomFidele").putNull("prenomFidele");r.putArray("datesCelebration").add(day.toString());return r;}
    @Test void dailySheetFiltersAsWebAndGroupsPaidValidatedIntentionsByHour()throws Exception{
        var rows=json.createArrayNode().add(request("Action de grâce", "08:30", "PAYE")).add(request("Prière pour la famille", "08:30", "PAYE")).add(request("Intention non payée", "10:00", "NON_PAYE")).add(request("Rejetée", "08:30", "PAYE").put("statutDemande","REJETEE")).add(request("Supprimée", "08:30", "PAYE").put("statusDel",true));
        var groups=IntentionSheet.groups(rows,json.createObjectNode(),day,false,List.of());assertEquals(1,groups.size());assertEquals(2,groups.get(0).intentions().size());assertEquals(LocalTime.of(8,30),groups.get(0).time());
        assertEquals(2,IntentionSheet.groups(rows,json.createObjectNode(),day,true,List.of()).size());assertEquals(1,IntentionSheet.groups(rows,json.createObjectNode(),day,true,List.of(LocalTime.of(10,0))).get(0).intentions().size());
        byte[] pdf=IntentionSheet.render(json.createObjectNode().put("nom","Saint Paul"),rows,json.createObjectNode(),day,false,List.of());try(var reader=new PdfReader(pdf)){String text=new PdfTextExtractor(reader).getTextFromPage(1);assertTrue(text.contains("FEUILLE D'INTENTIONS DE MESSES"));assertTrue(text.contains("DATE DE CÉLÉBRATION"));assertTrue(text.contains("08 h 30"));assertTrue(text.contains("Un(e) chrétien(ne)"));assertFalse(text.contains("Intention non payée"));assertFalse(text.contains("Rejetée"));assertFalse(text.contains("Supprimée"));}
        java.nio.file.Files.write(java.nio.file.Path.of("target","desktop-intention-preview.pdf"),pdf);
    }
    @Test void seriesUsesPerDateHourAndActualCelebrationFlagsWithoutInventingProgress(){
        var r=request("Une intention complète", "08:30", "PAYE");r.withArray("datesCelebration").removeAll().add("2027-01-11").add("2027-01-12").add("2027-01-13");var slots=r.putArray("celebrationSlots");slots.addObject().put("date","2027-01-11").put("ordre",1).put("heure","08:30").put("celebre",true);slots.addObject().put("date","2027-01-12").put("ordre",2).put("heure","07:00").put("celebre",false);slots.addObject().put("date","2027-01-13").put("ordre",3).put("heure","06:00").put("celebre",false);
        var group=IntentionSheet.groups(json.createArrayNode().add(r),json.createObjectNode(),day.plusDays(1),false,List.of()).get(0);assertEquals(LocalTime.of(7,0),group.time());assertTrue(group.intentions().get(0).progression().contains("2e célébration sur 3 (1 déjà célébrée)"));
    }
}
