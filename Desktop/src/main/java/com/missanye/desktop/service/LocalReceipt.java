package com.missanye.desktop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.missanye.desktop.model.ScreenFormats;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Mirrors the web receipt: one A4 sheet, original and duplicate. Transport state stays in the journal. */
public final class LocalReceipt {
    private static final Color INK=new Color(31,42,55), ACCENT=new Color(92,46,46), BORDER=new Color(208,213,221), MUTED=new Color(102,112,133);
    private final BaseFont regular,heavy;
    private LocalReceipt()throws Exception{regular=font("DejaVuSans.ttf");heavy=font("DejaVuSans-Bold.ttf");}
    public static byte[] render(JsonNode parish,JsonNode demand)throws Exception{return new LocalReceipt().create(parish,demand);}
    private Font text(float size,boolean bold,Color color){return new Font(bold?heavy:regular,size,Font.NORMAL,color);}
    private byte[] create(JsonNode parish,JsonNode demand)throws Exception{
        var bytes=new ByteArrayOutputStream();var document=new Document(PageSize.A4,20,20,12,12);var writer=PdfWriter.getInstance(document,bytes);
        document.addTitle("Reçu "+reference(demand));document.addAuthor(parish.path("nom").asText("Paroisse"));document.open();
        try{
            float width=PageSize.A4.getWidth()-40,half=(PageSize.A4.getHeight()-24-20)/2;
            watermark(writer,"ORIGINAL",PageSize.A4.getHeight()*.72f);watermark(writer,"DUPLICATA",PageSize.A4.getHeight()*.28f);
            var sheet=new PdfPTable(1);sheet.setTotalWidth(width);sheet.setLockedWidth(true);sheet.setSplitRows(false);sheet.setSplitLate(false);
            for(String label:new String[]{"ORIGINAL","DUPLICATA"}){
                var cell=new PdfPCell();cell.setBorder(0);cell.setPadding(0);cell.setFixedHeight(half);cell.addElement(block(parish,demand,label));sheet.addCell(cell);
                if("ORIGINAL".equals(label)){var cut=new PdfPCell(new Phrase("— — —  Découper ici  — — —",text(8,false,MUTED)));cut.setBorder(Rectangle.TOP);cut.setBorderColor(MUTED);cut.setHorizontalAlignment(Element.ALIGN_CENTER);cut.setVerticalAlignment(Element.ALIGN_MIDDLE);cut.setFixedHeight(20);sheet.addCell(cut);}
            }
            document.add(sheet);
        }finally{document.close();}return bytes.toByteArray();
    }
    private PdfPTable block(JsonNode parish,JsonNode demand,String copy)throws Exception{
        var block=new PdfPTable(1);block.setWidthPercentage(100);
        var badge=cell(copy,9,true,MUTED);badge.setHorizontalAlignment(Element.ALIGN_RIGHT);block.addCell(badge);
        var identity=new PdfPTable(new float[]{1,5});identity.setWidthPercentage(100);
        Image logo=null;try{String encoded=parish.path("logoBase64").asText();if(!encoded.isBlank()){logo=Image.getInstance(Base64.getDecoder().decode(encoded));logo.scaleToFit(42,42);}}catch(Exception ignored){/* Same fallback as the web receipt. */}
        if(logo!=null){var image=new PdfPCell(logo,false);image.setBorder(0);image.setHorizontalAlignment(Element.ALIGN_CENTER);image.setVerticalAlignment(Element.ALIGN_MIDDLE);identity.addCell(image);}
        var name=new PdfPCell();name.setBorder(0);if(logo==null)name.setColspan(2);
        centered(name,"ARCHIDIOCESE DE LOME",10,INK);centered(name,truncate(parish.path("nom").asText("Paroisse"),80),12,ACCENT);
        var contact=new ArrayList<String>();for(String key:new String[]{"adresse","telephone","email"})if(!parish.path(key).asText("").isBlank())contact.add(parish.path(key).asText(""));
        if(!contact.isEmpty()){var p=new Paragraph(truncate(String.join(" · ",contact),115),text(8,false,MUTED));p.setAlignment(Element.ALIGN_CENTER);name.addElement(p);}identity.addCell(name);
        var heading=new PdfPCell(identity);heading.setPadding(9);heading.setBackgroundColor(new Color(245,241,235));heading.setBorderColor(BORDER);block.addCell(heading);
        var title=cell("REÇU DE DÉPÔT",12,true,Color.WHITE);title.setHorizontalAlignment(Element.ALIGN_CENTER);title.setBackgroundColor(INK);title.setPadding(7);block.addCell(title);
        var highlights=new PdfPTable(new float[]{1,1});highlights.setWidthPercentage(100);
        block.addCell(highlight("Code de suivi",reference(demand),12));highlights.addCell(highlight("Montant",ScreenFormats.money(demand.path("localQuotedMontant")),11));highlights.addCell(highlight("Dépôt",date(demand.path("localRecordedAt").asText()),10));block.addCell(wrap(highlights));
        var intention=cell(truncate(demand.path("intention").asText(),160),11,true,INK);intention.setBackgroundColor(new Color(252,248,240));intention.setPadding(7);block.addCell(intention);
        var meta=new PdfPTable(new float[]{1.2f,3.3f});meta.setWidthPercentage(100);
        meta(meta,"Célébration",celebration(demand));meta(meta,"Type",demand.path("receiptType").asText(demand.path("typeDemandeLibelle").asText("—")));
        meta(meta,"Demandeur",ScreenFormats.faithful(demand)+(demand.path("telFidele").asText().isBlank()?"":" · "+demand.path("telFidele").asText()));
        boolean cash=demand.path("localCashReceived").asBoolean();String mode=demand.path("typePaiementLibelle").asText(demand.path("receiptPayment").asText("—"));
        String status=switch(demand.path("statutPaiement").asText()){case "PAYE"->"Payée";case "EN_ATTENTE"->"En attente";case "ECHOUE"->"Échouée";default->"Non payée";};
        String payment=cash?"Espèces · "+parish.path("nom").asText("Paroisse")+" · Payée":mode+" · "+status;
        meta(meta,"Paiement",payment);if(!demand.path("refFacture").asText().isBlank())meta(meta,"Réf. facture",demand.path("refFacture").asText());block.addCell(wrap(meta));
        var notice=cell("Conservez ce reçu. Code de suivi pour le suivi.",9,false,MUTED);notice.setPaddingTop(4);block.addCell(notice);return block;
    }
    private void centered(PdfPCell cell,String value,int size,Color color){var p=new Paragraph(value,text(size,true,color));p.setAlignment(Element.ALIGN_CENTER);cell.addElement(p);}
    private PdfPCell cell(String value,int size,boolean bold,Color color){var c=new PdfPCell(new Phrase(value,text(size,bold,color)));c.setBorder(0);c.setPadding(2);return c;}
    private PdfPCell wrap(PdfPTable table){var c=new PdfPCell(table);c.setBorder(0);c.setPadding(0);return c;}
    private PdfPCell highlight(String label,String value,int size){var inner=new PdfPTable(1);inner.addCell(cell(label,8,false,MUTED));inner.addCell(cell(value,size,true,INK));var c=new PdfPCell(inner);c.setPadding(6);c.setBackgroundColor(new Color(248,250,252));c.setBorderColor(BORDER);return c;}
    private void meta(PdfPTable table,String label,String value){table.addCell(cell(label,9,true,MUTED));table.addCell(cell(truncate(value,120),9,false,INK));}
    private static String reference(JsonNode demand){return demand.path("localReference").asText(demand.path("codeSuivie").asText());}
    private static String celebration(JsonNode demand){var dates=new ArrayList<String>();if(demand.path("datesCelebration").isArray())for(var d:demand.path("datesCelebration"))dates.add(ScreenFormats.date(d.asText()));if(dates.isEmpty())dates.add(ScreenFormats.date(demand.path("dateDebut").asText()));String value=String.join(" · ",dates.subList(0,Math.min(4,dates.size())));if(dates.size()>4)value+=" · +"+(dates.size()-4);String time=demand.path("heurePersonnalisee").asText(demand.path("receiptTime").asText());return value+(time.isBlank()?"":" · "+formatTime(time));}
    private static String formatTime(String value){try{return LocalTime.parse(value).format(DateTimeFormatter.ofPattern("HH' h 'mm"));}catch(Exception invalid){return value;}}
    private void watermark(PdfWriter writer,String label,float y){var canvas=writer.getDirectContentUnder();canvas.saveState();var gs=new PdfGState();gs.setFillOpacity(.08f);canvas.setGState(gs);canvas.setColorFill(new Color(90,90,90));canvas.beginText();canvas.setFontAndSize(heavy,36);canvas.showTextAligned(Element.ALIGN_CENTER,label,PageSize.A4.getWidth()/2,y,28);canvas.endText();canvas.restoreState();}
    private static String truncate(String value,int max){if(value==null||value.isBlank())return "—";return value.length()<=max?value:value.substring(0,max-1).trim()+"…";}
    private static String date(String value){var f=DateTimeFormatter.ofPattern("dd/MM/yyyy HH' h 'mm");try{return Instant.parse(value).atZone(ZoneId.of("Africa/Lome")).format(f);}catch(Exception ignored){try{return LocalDateTime.parse(value).format(f);}catch(Exception invalid){return "—";}}}
    static BaseFont font(String name)throws Exception{try(var stream=LocalReceipt.class.getResourceAsStream("/com/missanye/desktop/branding/fonts/"+name)){if(stream==null)throw new java.io.IOException("Police du reçu introuvable");return BaseFont.createFont(name,BaseFont.IDENTITY_H,BaseFont.EMBEDDED,true,stream.readAllBytes(),null);}}
}
