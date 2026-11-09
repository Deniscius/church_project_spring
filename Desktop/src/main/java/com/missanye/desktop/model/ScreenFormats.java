package com.missanye.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.text.NumberFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** User-facing labels shared by tables, details and activity messages. */
public final class ScreenFormats {
    private ScreenFormats() {}
    public static String state(String state){return switch(state){case "DRAFT"->"Brouillon";case "PENDING"->"À transmettre";case "SENT"->"Confirmée";case "REJECTED"->"À corriger";case "PAYE"->"Payé";case "NON_PAYE"->"À régler";case "EN_ATTENTE"->"En attente";case "VALIDEE"->"Validée";case "REJETEE"->"Rejetée";case "ANNULEE"->"Annulée";case "TERMINEE"->"Terminée";default->state;};}
    public static String kind(String kind){return switch(kind){case "CREATE_DEMANDE"->"Nouvelle demande";case "CASH_PAYMENT"->"Encaissement espèces";case "UPDATE_INTENTION"->"Correction d'intention";case "VALIDATE_DEMANDE"->"Décision";case "DELETE_DEMANDE"->"Suppression";default->"Opération";};}
    public static String money(JsonNode amount){if(!amount.isNumber())return "—";var format=NumberFormat.getNumberInstance(Locale.FRANCE);format.setMaximumFractionDigits(0);return format.format(amount.decimalValue())+" F CFA";}
    public static String faithful(JsonNode demand){String name=(demand.path("prenomFidele").asText()+" "+demand.path("nomFidele").asText()).trim();return name.isEmpty()?"Non renseigné":name;}
    public static String date(String date){try{return LocalDate.parse(date.substring(0,10)).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));}catch(Exception e){return "—";}}
    public static String reference(JsonNode entry){return entry.path("result").path("codeSuivie").asText(entry.path("codeSuivie").asText("En cours de création"));}
}
