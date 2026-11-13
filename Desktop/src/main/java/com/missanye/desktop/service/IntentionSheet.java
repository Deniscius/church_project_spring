package com.missanye.desktop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.missanye.desktop.model.ScreenFormats;
import com.missanye.printing.CelebrationSheetPdf;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Same daily selection and grouping as FeuilleCelebrationService. Rendering is shared with the web. */
public final class IntentionSheet {
    private IntentionSheet(){}
    private record Row(LocalTime time,String label,String intention,String faithful,String progression,boolean celebrated){}
    public static List<CelebrationSheetPdf.Mass> groups(JsonNode demands,JsonNode catalogue,LocalDate date,boolean includeUnpaid,List<LocalTime> selectedHours){
        var rows=new ArrayList<Row>();
        for(var demand:demands){
            if(demand.path("statusDel").asBoolean())continue;
            if(demand.path("localReconciliationRequired").asBoolean()&&!"PAYE".equals(demand.path("statutPaiement").asText()))continue;
            String state=demand.path("statutDemande").asText("");String validation=demand.path("localValidation").asText(demand.path("statutValidation").asText(""));
            boolean local=demand.path("localPending").asBoolean();if(!Set.of("VALIDEE","TERMINEE").contains(state)&&!(local&&"VALIDEE".equals(validation)&&(state.isBlank()||"EN_ATTENTE".equals(state))))continue;
            if("REJETEE".equals(validation))continue;
            boolean paid="PAYE".equals(demand.path("statutPaiement").asText())||demand.path("localCashReceived").asBoolean();if(!includeUnpaid&&!paid)continue;
            var dates=new ArrayList<String>();for(var d:demand.path("datesCelebration"))dates.add(d.asText());if(dates.isEmpty()&&!demand.path("dateDebut").asText("").isBlank())dates.add(demand.path("dateDebut").asText());Collections.sort(dates);
            int index=dates.indexOf(date.toString());if(index<0)continue;JsonNode slot=demand;for(var s:demand.path("celebrationSlots"))if(date.toString().equals(s.path("date").asText())){slot=s;break;}
            LocalTime time=time(slot.path("heure").asText(slot.path("heurePersonnalisee").asText(demand.path("heurePersonnalisee").asText(demand.path("horaireHeure").asText("")))));
            String scheduleId=slot.path("horairePublicId").asText(demand.path("horairePublicId").asText(""));if(time==null)for(var h:catalogue.path("horaires"))if(scheduleId.equals(h.path("publicId").asText()))time=time(h.path("heureCelebration").asText(""));
            if(!selectedHours.isEmpty()&&(time==null||!selectedHours.contains(time)))continue;
            String label=time==null?"Messe — heure non précisée":"Messe de "+format(time);LocalTime main=time(demand.path("horaireHeure").asText(""));String mainLabel=demand.path("horaireLibelle").asText("");
            if(main==null)for(var h:catalogue.path("horaires"))if(demand.path("horairePublicId").asText("").equals(h.path("publicId").asText())){main=time(h.path("heureCelebration").asText(""));mainLabel=h.path("libelle").asText("");}
            if(time!=null&&time.equals(main)&&!mainLabel.isBlank())label=mainLabel+" ("+format(time)+")";
            boolean celebrated=slot.path("celebre").asBoolean();int count=dates.size(),order=slot.path("ordre").asInt(index+1),done=0;boolean known=demand.path("publicId").asText().startsWith("local:");
            for(var s:demand.path("celebrationSlots")){if(s.has("celebre"))known=true;if(s.path("celebre").asBoolean())done++;}
            String progression=celebrated?"Célébrée":"";if(count>1){String duration=switch(count){case 3->"Triduum";case 9->"Neuvaine";case 30->"Trentaine";default->count+" célébrations";};progression=duration+" — "+(order==1?"1ère":order+"e")+" célébration sur "+count+(known?" ("+done+" déjà célébrée"+(done>1?"s":"")+")":"")+(celebrated?" — terminée":"");}
            rows.add(new Row(time,label,demand.path("intention").asText(""),ScreenFormats.faithful(demand),progression,celebrated));
        }
        rows.sort(Comparator.comparing(Row::time,Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(Row::intention,String.CASE_INSENSITIVE_ORDER));var grouped=new LinkedHashMap<LocalTime,List<Row>>();for(var row:rows)grouped.computeIfAbsent(row.time(),k->new ArrayList<>()).add(row);
        var result=new ArrayList<CelebrationSheetPdf.Mass>();for(var entry:grouped.entrySet()){var group=entry.getValue();result.add(new CelebrationSheetPdf.Mass(entry.getKey(),group.stream().map(Row::label).filter(label->label.endsWith(")")).findFirst().orElse(group.get(0).label()),group.stream().map(r->new CelebrationSheetPdf.Intention(r.intention(),r.faithful(),r.progression(),r.celebrated())).toList()));}return result;
    }
    public static byte[] render(JsonNode branding,JsonNode demands,JsonNode catalogue,LocalDate date,boolean includeUnpaid,List<LocalTime> selectedHours){
        byte[] logo=null;try{if(!branding.path("logoBase64").asText("").isBlank())logo=Base64.getDecoder().decode(branding.path("logoBase64").asText());}catch(Exception ignored){}
        return CelebrationSheetPdf.render(new CelebrationSheetPdf.Branding(branding.path("nom").asText("Paroisse"),branding.path("email").asText(""),branding.path("telephone").asText(""),logo),date,includeUnpaid,selectedHours,groups(demands,catalogue,date,includeUnpaid,selectedHours));
    }
    private static LocalTime time(String text){try{return LocalTime.parse(text).withSecond(0).withNano(0);}catch(Exception invalid){return null;}}
    private static String format(LocalTime time){return time.format(DateTimeFormatter.ofPattern("HH' h 'mm"));}
}
