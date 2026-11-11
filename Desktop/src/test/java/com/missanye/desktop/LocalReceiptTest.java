package com.missanye.desktop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.missanye.desktop.service.LocalReceipt;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LocalReceiptTest {
    private final ObjectMapper json=new ObjectMapper();
    private ObjectNode demand(){return TestRequests.complete().put("localReference","MSL-731C7AB4-2FAE-4D78-A17B-0FF7653F4501").put("localRecordedAt","2026-11-10T10:00:00Z").put("localQuotedMontant",5000).put("nomFidele","AMEGA").put("prenomFidele","Alice").put("receiptType","Messe").put("statutPaiement","NON_PAYE");}
    @Test void twoCopiesStayOnOneSheetWithLongTextAndDoNotAttestUnreceivedMoney()throws Exception{
        var branding=json.createObjectNode().put("nom","Paroisse Notre-Dame de la Paix").put("telephone","+22890000000").put("email","paroisse@example.test");
        try(var logo=LocalReceiptTest.class.getResourceAsStream("/com/missanye/desktop/branding/missanye.png")){branding.put("logoBase64",java.util.Base64.getEncoder().encodeToString(logo.readAllBytes()));}
        var demand=demand().put("intention","Une longue intention pour toute la famille et les proches. ".repeat(30));
        var dates=demand.putArray("datesCelebration");for(int day=1;day<=30;day++)dates.add("2027-01-"+String.format("%02d",day));
        try(var reader=new PdfReader(LocalReceipt.render(branding,demand))){
            assertEquals(1,reader.getNumberOfPages());assertTrue(reader.getPageN(1).getAsDict(com.lowagie.text.pdf.PdfName.RESOURCES).getAsDict(com.lowagie.text.pdf.PdfName.XOBJECT).size()>0);String content=new PdfTextExtractor(reader).getTextFromPage(1);
            assertTrue(content.contains("ORIGINAL"));assertTrue(content.contains("DUPLICATA"));assertTrue(content.contains("Non payée"));assertTrue(content.contains("+26"));assertTrue(content.contains("paroisse@example.test"));assertTrue(content.contains("Alice AMEGA"));
            assertFalse(content.contains("Payée"));assertFalse(content.contains("TRANSMISSION"));assertFalse(content.contains("LOCAL"));
        }
    }
    @Test void cashReceiptHasSameClientContentBeforeAndAfterTransmission()throws Exception{
        var branding=json.createObjectNode().put("nom","Saint Paul");var demand=demand().put("localCashReceived",true).put("localPending",true);
        String before,after;
        try(var reader=new PdfReader(LocalReceipt.render(branding,demand))){before=new PdfTextExtractor(reader).getTextFromPage(1);}
        demand.put("localPending",false).put("statutPaiement","PAYE").put("codeSuivie","MS-SP-ABCDEFGHJK");
        try(var reader=new PdfReader(LocalReceipt.render(branding,demand))){after=new PdfTextExtractor(reader).getTextFromPage(1);}
        assertEquals(before,after);assertTrue(before.contains("Espèces · Saint Paul · Payée"));assertTrue(before.contains(demand.path("localReference").asText()));
    }
}
