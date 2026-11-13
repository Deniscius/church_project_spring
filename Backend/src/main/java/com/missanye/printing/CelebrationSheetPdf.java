package com.missanye.printing;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** The single print template used by the web backend and the JavaFX desktop. No Spring/JPA dependency. */
public final class CelebrationSheetPdf {
    public record Branding(String name,String email,String phone,byte[] logo){}
    public record Intention(String text,String faithful,String progression,boolean celebrated){}
    public record Mass(LocalTime time,String label,List<Intention> intentions){}
    private static final Color HEADER=new Color(31,42,55),ACCENT=new Color(92,46,46),SECTION=new Color(245,241,235),BORDER=new Color(208,213,221),MUTED=new Color(102,112,133);
    private static final DateTimeFormatter TIME=DateTimeFormatter.ofPattern("HH' h 'mm");
    private final BaseFont regular,bold;
    private CelebrationSheetPdf()throws Exception{regular=font("DejaVuSans.ttf");bold=font("DejaVuSans-Bold.ttf");}
    private Font f(int size,boolean strong,Color color){return new Font(strong?bold:regular,size,Font.NORMAL,color);}
    public static byte[] render(Branding branding,LocalDate date,boolean includeUnpaid,List<LocalTime> hours,List<Mass> masses){
        try{return new CelebrationSheetPdf().create(branding,date,includeUnpaid,hours,masses);}catch(Exception e){throw new IllegalStateException("Impossible de générer la feuille d'intentions PDF",e);}
    }
    private byte[] create(Branding branding,LocalDate date,boolean include,List<LocalTime> hours,List<Mass> masses)throws Exception{
        var bytes=new ByteArrayOutputStream();var document=new Document(PageSize.A4,36,36,36,40);PdfWriter.getInstance(document,bytes);document.addTitle("Feuille d'intentions — "+date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));document.addAuthor(branding.name());document.open();
        try{
            document.add(header(branding));var celebration=new PdfPTable(1);celebration.setWidthPercentage(100);celebration.setSpacingAfter(12);
            var cell=new PdfPCell();cell.setPadding(10);cell.setBackgroundColor(SECTION);cell.setBorderColor(BORDER);var label=new Paragraph("DATE DE CÉLÉBRATION",f(10,true,MUTED));label.setAlignment(Element.ALIGN_CENTER);cell.addElement(label);
            var value=new Paragraph(capitalize(date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy",Locale.FRENCH))),f(13,true,HEADER));value.setAlignment(Element.ALIGN_CENTER);value.setSpacingBefore(3);cell.addElement(value);celebration.addCell(cell);document.add(celebration);
            int total=masses.stream().mapToInt(m->m.intentions().size()).sum();String note=include?"Intentions validées (payées et non payées).":"Intentions validées et payées uniquement.";
            if(!hours.isEmpty())note+="  ·  Messes de "+hours.stream().sorted().map(TIME::format).collect(java.util.stream.Collectors.joining(", "))+".";
            var summary=new Paragraph(note+"  ·  "+total+" intention"+(total>1?"s":"")+"  ·  "+masses.size()+" messe"+(masses.size()>1?"s":"")+".",f(8,false,MUTED));summary.setAlignment(Element.ALIGN_CENTER);summary.setSpacingAfter(10);document.add(summary);
            if(masses.isEmpty()){var empty=new Paragraph("Aucune intention à célébrer pour cette date de célébration.",f(11,false,HEADER));empty.setAlignment(Element.ALIGN_CENTER);document.add(empty);}
            else{
                var plan=new Paragraph("Plan des messes : "+masses.stream().map(m->m.label()+" ("+m.intentions().size()+")").collect(java.util.stream.Collectors.joining("  ·  ")),f(8,false,MUTED));plan.setAlignment(Element.ALIGN_CENTER);plan.setSpacingAfter(12);document.add(plan);
                for(var mass:masses){var banner=new PdfPTable(1);banner.setWidthPercentage(100);banner.setSpacingBefore(12);banner.setSpacingAfter(8);var heading=new PdfPCell(new Phrase(mass.label().toUpperCase(Locale.FRENCH)+"  —  "+mass.intentions().size()+" intention"+(mass.intentions().size()>1?"s":""),f(9,true,Color.WHITE)));heading.setPadding(10);heading.setBackgroundColor(ACCENT);heading.setBorderColor(ACCENT);banner.addCell(heading);document.add(banner);
                    int rank=1;for(var intention:mass.intentions())document.add(card(mass.time(),intention,rank++));
                }
            }
            var footer=new Paragraph("Document destiné à la sacristie et au célébrant. Les intentions sont regroupées par heure de messe. La date ci-dessus est celle de la célébration (distincte de la date de dépôt).",f(8,false,MUTED));footer.setSpacingBefore(16);document.add(footer);
        }finally{document.close();}return bytes.toByteArray();
    }
    private PdfPTable header(Branding data)throws Exception{
        var table=new PdfPTable(1);table.setWidthPercentage(100);table.setSpacingAfter(12);var identity=new PdfPCell();identity.setPadding(12);identity.setBorderColor(BORDER);identity.setBackgroundColor(SECTION);
        var row=new PdfPTable(new float[]{1.2f,4.8f});row.setWidthPercentage(100);Image logo=null;try{if(data.logo()!=null&&data.logo().length>0){logo=Image.getInstance(data.logo());logo.scaleToFit(72,72);}}catch(Exception ignored){}
        if(logo!=null){var image=new PdfPCell(logo,false);image.setBorder(0);image.setHorizontalAlignment(Element.ALIGN_CENTER);image.setVerticalAlignment(Element.ALIGN_MIDDLE);row.addCell(image);}
        var text=new PdfPCell();text.setBorder(0);if(logo==null)text.setColspan(2);center(text,"ARCHIDIOCESE DE LOME",11,true,HEADER);center(text,blank(data.name())?"Paroisse":data.name(),12,true,ACCENT);
        String contact=blank(data.email())?data.phone():blank(data.phone())?data.email():data.email()+" – "+data.phone();if(!blank(contact))center(text,contact,9,false,MUTED);row.addCell(text);identity.addElement(row);table.addCell(identity);
        var title=new PdfPCell();title.setBackgroundColor(HEADER);title.setBorderColor(HEADER);title.setPadding(10);center(title,"FEUILLE D'INTENTIONS DE MESSES",15,true,Color.WHITE);table.addCell(title);return table;
    }
    private PdfPTable card(LocalTime time,Intention intention,int index){
        var table=new PdfPTable(new float[]{1.35f,3.15f});table.setWidthPercentage(100);table.setSpacingAfter(10);table.setSplitLate(false);
        var rank=new PdfPCell(new Phrase("Intention n° "+index+(time==null?"":"  ·  "+time.format(TIME)),f(9,true,Color.WHITE)));rank.setColspan(2);rank.setPadding(7);rank.setBackgroundColor(HEADER);rank.setBorderColor(HEADER);table.addCell(rank);
        var body=new PdfPCell(new Phrase(intention.text(),f(12,true,HEADER)));body.setColspan(2);body.setPadding(12);body.setBackgroundColor(new Color(252,248,240));body.setBorderColor(BORDER);table.addCell(body);
        meta(table,"Demandeur",blank(intention.faithful())?"Un(e) chrétien(ne)":intention.faithful());if(!blank(intention.progression()))meta(table,"Progression",intention.progression());if(intention.celebrated())meta(table,"Statut","Célébrée");return table;
    }
    private void meta(PdfPTable table,String key,String value){var label=new PdfPCell(new Phrase(key,f(10,true,MUTED)));label.setPadding(8);label.setBorderColor(BORDER);table.addCell(label);var body=new PdfPCell(new Phrase(value,f(11,false,HEADER)));body.setPadding(8);body.setBorderColor(BORDER);table.addCell(body);}
    private void center(PdfPCell cell,String text,int size,boolean strong,Color color){var paragraph=new Paragraph(text,f(size,strong,color));paragraph.setAlignment(Element.ALIGN_CENTER);cell.addElement(paragraph);}
    private static boolean blank(String value){return value==null||value.isBlank();}
    private static String capitalize(String text){return Character.toUpperCase(text.charAt(0))+text.substring(1);}
    private static BaseFont font(String name)throws Exception{try(var in=CelebrationSheetPdf.class.getResourceAsStream("/com/missanye/printing/fonts/"+name)){if(in==null)throw new java.io.IOException("Police du document absente");return BaseFont.createFont(name,BaseFont.IDENTITY_H,BaseFont.EMBEDDED,true,in.readAllBytes(),null);}}
}
