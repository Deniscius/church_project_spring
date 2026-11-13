package com.missanye.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.text.NumberFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** User-facing labels shared by tables, details and activity messages. */
public final class ScreenFormats {
    private ScreenFormats() {}
    public static String state(String state){return switch(state){case "DRAFT"->"Brouillon";case "PENDING"->"Enregistrée";case "SENT"->"Confirmée";case "REJECTED"->"À corriger";case "BLOCKED"->"Bloquée";case "SUPERSEDED"->"Remplacée";case "ESPECES_ENREGISTREES"->"Payé";case "PAYE"->"Payé";case "NON_PAYE"->"À régler";case "EN_ATTENTE"->"En attente";case "VALIDEE"->"Validée";case "REJETEE"->"Rejetée";case "ANNULEE"->"Annulée";case "TERMINEE"->"Terminée";default->state;};}
    public static String kind(String kind){return switch(kind){case "CREATE_DEMANDE"->"Nouvelle demande";case "CASH_PAYMENT"->"Encaissement espèces";case "UPDATE_INTENTION"->"Correction d'intention";case "VALIDATE_DEMANDE"->"Décision";case "DELETE_DEMANDE"->"Suppression";default->"Opération";};}
    public static String money(JsonNode amount){if(!amount.isNumber())return "—";var format=NumberFormat.getNumberInstance(Locale.FRANCE);format.setMaximumFractionDigits(0);return format.format(amount.decimalValue())+" F CFA";}
    public static String faithful(JsonNode demand){String name=(demand.path("prenomFidele").asText("").trim()+" "+demand.path("nomFidele").asText("").trim()).trim();return name.isEmpty()?"Un(e) chrétien(ne)":name;}
    public static boolean matchesDemand(JsonNode demand,String query){
        if(query==null||query.isBlank())return true;String text=query.trim().toLowerCase(Locale.ROOT);
        String searchable=reference(demand)+" "+faithful(demand)+" "+demand.path("telFidele").asText("")+" "+demand.path("intention").asText("")+" "+demand.path("typeDemandeLibelle").asText("");if(searchable.toLowerCase(Locale.ROOT).contains(text))return true;
        String number=text.replaceAll("[^0-9]","");return number.length()>=4&&demand.path("telFidele").asText().replaceAll("[^0-9]","").contains(number);
    }
    public static String details(JsonNode demand){
        var dates=new ArrayList<String>();for(var d:demand.path("datesCelebration"))dates.add(date(d.asText("")));if(dates.isEmpty()&&!demand.path("dateDebut").asText("").isBlank())dates.add(date(demand.path("dateDebut").asText("")));
        return "Code de suivi : "+reference(demand)+"\nFidèle : "+faithful(demand)+"\nTéléphone : "+demand.path("telFidele").asText("")+"\nMontant : "+money(demand.path("montant"))+"\nPaiement : "+state(demand.path("statutPaiement").asText(""))+"\nCélébrations : "+String.join(", ",dates)+"\nIntention : "+demand.path("intention").asText("");
    }
    public static String role(String role){return switch(role){case "SUPER_ADMIN"->"Administrateur plateforme";case "ADMIN"->"Administrateur";case "SECRETAIRE"->"Secrétaire";case "CURE"->"Curé";case "COMPTABLE_LOCAL"->"Comptable paroissial";case "COMPTABLE"->"Comptable";default->role.replace('_',' ');};}
    public static String date(String date){try{return LocalDate.parse(date.substring(0,10)).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));}catch(Exception e){return "—";}}
    public static String reference(JsonNode entry){String local=entry.path("localReference").asText();return local.isBlank()?entry.path("result").path("codeSuivie").asText(entry.path("codeSuivie").asText("En cours de création")):local;}
}
