package com.missanye.desktop.service;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.missanye.desktop.api.ApiClient;
import java.io.IOException;
import java.util.*;

/** Online preparation is completed before an account is advertised as offline-ready. */
public final class WorkspaceBootstrap {
    private static final ObjectMapper JSON=new ObjectMapper();
    private WorkspaceBootstrap(){}
    public record Prepared(JsonNode session,JsonNode catalogue,JsonNode demands){}
    public static ArrayNode eligible(JsonNode parishes){
        var result=JSON.createArrayNode();
        if(!parishes.isArray())return result;
        for(var p:parishes){
            String id=p.path("id").asText(p.path("publicId").asText());
            if(id.isBlank()||!p.path("active").asBoolean(true)||!p.path("isActive").asBoolean(true)||p.path("statusDel").asBoolean()||p.path("isSystem").asBoolean())continue;
            try{UUID.fromString(id);}catch(Exception e){continue;}
            result.add(p);
        }
        return result;
    }
    public static String parishId(JsonNode parish){return parish.path("id").asText(parish.path("publicId").asText());}
    public static boolean allows(JsonNode parishes,String id){for(var p:eligible(parishes))if(id.equals(parishId(p)))return true;return false;}
    public static Prepared fetch(ApiClient api,JsonNode authenticated,String parish)throws Exception{
        UUID.fromString(parish);var current=api.get("auth/me");
        if(!authenticated.path("user").path("id").asText().equals(current.path("user").path("id").asText()))throw new SyncService.SessionMismatchException();
        if(!current.path("user").path("isGlobal").asBoolean()&&!allows(current.path("paroisses"),parish))throw new IllegalArgumentException("Votre accès à cette paroisse a changé. Reconnectez-vous pour vérifier vos accès.");
        return new Prepared(current,catalogue(api,parish),demands(api,parish));
    }
    public static JsonNode catalogue(ApiClient api,String parish)throws Exception{
        UUID.fromString(parish);var types=api.get("type-demandes/paroisse/"+parish);var schedules=api.get("horaires/paroisse/"+parish);var payments=api.get("type-paiement");
        if(!types.isArray()||!schedules.isArray()||!payments.isArray())throw new IOException("Catalogue de paroisse incomplet");
        var result=JSON.createObjectNode();result.set("types",types);result.set("horaires",schedules);result.set("paiements",payments);var prices=result.putObject("forfaits");
        for(var type:types){String id=type.path("publicId").asText();UUID.fromString(id);var values=api.get("forfait-tarifs/type-demande/"+id+"/actifs");if(!values.isArray())throw new IOException("Catalogue de tarifs incomplet");prices.set(id,values);}
        return result;
    }
    public static ArrayNode demands(ApiClient api,String parish)throws Exception{
        UUID.fromString(parish);var rows=JSON.createArrayNode();
        for(int page=0;;page++){
            var response=api.get("demandes/paroisse/"+parish+"?size=100&page="+page);
            if(!response.path("content").isArray()||!response.path("last").isBoolean())throw new IOException("Liste des demandes incomplète");
            response.path("content").forEach(rows::add);if(response.path("last").asBoolean())return rows;
            if(page>=9999)throw new IOException("Pagination trop importante");
        }
    }
}
