package com.missanye.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.*;
import java.util.*;

/** Local drafts may be incomplete; only a complete request may enter the outbox. */
public final class RequestValidation {
    private RequestValidation() {}
    public static ObjectNode forSubmission(JsonNode stored) {
        if(!(stored instanceof ObjectNode))throw new IllegalArgumentException("Le brouillon est introuvable.");
        ObjectNode request=stored.deepCopy();request.put("intention",request.path("intention").asText().trim());
        String rawTime=request.path("_customTimeText").asText(request.path("heurePersonnalisee").asText(""));
        int expected=request.path("_expectedCelebrations").asInt(0);
        request.remove(List.of("_customTimeText","_expectedCelebrations","_invalidDateText"));
        var slots=request.path("celebrationSlots");
        if(slots.isArray()&&!slots.isEmpty()){
            request.remove(List.of("heurePersonnalisee","horairePublicId"));var first=slots.get(0);
            if(first.hasNonNull("heurePersonnalisee"))request.set("heurePersonnalisee",first.get("heurePersonnalisee"));else request.set("horairePublicId",first.get("horairePublicId"));
        }else if(!rawTime.isBlank()){
            try{request.put("heurePersonnalisee",LocalTime.parse(rawTime.trim()).toString());request.remove("horairePublicId");}
            catch(Exception e){throw new IllegalArgumentException("Saisissez une heure valide, par exemple 06:30.");}
        }
        if(expected>0&&request.path("datesCelebration").size()!=expected)fail("Le forfait exige "+expected+" date(s) distincte(s).");
        readyToSend(request);return request;
    }
    public static void readyToSend(JsonNode request) {
        String intention=request.path("intention").asText().trim();
        if(intention.length()<10||intention.length()>500)fail("L'intention doit contenir entre 10 et 500 caractères.");
        if(!request.path("telFidele").asText().matches("\\+[1-9]\\d{6,14}"))fail("Renseignez un téléphone international, par exemple +22890123456.");
        for(String field:List.of("paroissePublicId","typeDemandePublicId","forfaitTarifPublicId","typePaiementPublicId"))uuid(request.path(field).asText(),switch(field){case "paroissePublicId"->"paroisse";case "typeDemandePublicId"->"type de demande";case "forfaitTarifPublicId"->"forfait";default->"mode de paiement";});
        limit(request,"nomFidele",100,"nom");limit(request,"prenomFidele",100,"prénom");limit(request,"nomCoursier",100,"coursier");limit(request,"emailFidele",150,"adresse e-mail");
        String email=request.path("emailFidele").asText();if(!email.isBlank()&&!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))fail("Vérifiez l'adresse e-mail.");
        LocalDate first=date(request.path("dateDebut").asText());
        var dates=new TreeSet<LocalDate>();for(var d:request.path("datesCelebration"))if(!dates.add(date(d.asText())))fail("Une date de célébration apparaît plusieurs fois.");
        if(dates.isEmpty())dates.add(first);
        if(dates.size()>30||!dates.first().equals(first))fail("Vérifiez la première date et les dates supplémentaires.");
        if(request.path("celebrationSlots").isArray()&&!request.path("celebrationSlots").isEmpty()){
            var slots=new TreeSet<LocalDate>();for(var slot:request.path("celebrationSlots")){if(!slots.add(date(slot.path("date").asText())))fail("Un créneau apparaît plusieurs fois.");timeOrSchedule(slot);}
            if(!slots.equals(dates))fail("Les créneaux ne correspondent plus aux dates choisies. Reconfigurez les horaires.");
        }else timeOrSchedule(request);
    }
    private static void timeOrSchedule(JsonNode node){String time=node.path("heurePersonnalisee").asText("");if(time.isBlank())uuid(node.path("horairePublicId").asText(),"horaire");else try{LocalTime.parse(time);}catch(Exception e){fail("Vérifiez l'heure de célébration.");}}
    private static LocalDate date(String text){try{return LocalDate.parse(text);}catch(Exception e){throw new IllegalArgumentException("Choisissez une date de célébration valide.");}}
    private static void uuid(String text,String label){try{UUID.fromString(text);}catch(Exception e){fail("Le champ « "+label+" » doit être choisi dans les options de votre paroisse.");}}
    private static void limit(JsonNode node,String field,int max,String label){if(node.path(field).asText().length()>max)fail("Le champ "+label+" ne doit pas dépasser "+max+" caractères.");}
    private static void fail(String message){throw new IllegalArgumentException(message);}
}
