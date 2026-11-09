package com.missanye.desktop.service;

import com.missanye.desktop.api.ApiClient;
import com.missanye.desktop.repository.LocalStore;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;

/** Called on a serial background executor. Transport errors leave the same UUID pending. */
public final class SyncService {
    private final LocalStore store;
    private final ApiClient api;
    private final ObjectMapper json=new ObjectMapper();
    public SyncService(LocalStore store,ApiClient api){this.store=store;this.api=api;}
    public void verifySession() throws Exception {
        String id=api.get("auth/me").path("user").path("id").asText();
        if(!store.owner().equals(id))throw new IllegalArgumentException("La session ne correspond pas au propriétaire du coffre.");
    }
    public String synchronize() throws Exception {
        verifySession(); int sent=0,rejected=0;
        for(var entry:store.entries()) {
            if(!"PENDING".equals(entry.path("state").asText()))continue;
            // Persist attempt before I/O. Never generate a new key after an uncertain result.
            entry.put("attempts",entry.path("attempts").asInt()+1);store.put("entry:"+entry.path("id").asText(),entry);
            var command=json.createObjectNode().put("operationId",entry.path("id").asText()).put("kind",entry.path("kind").asText());command.set("payload",entry.path("payload"));
            try {
                var result=api.post("desktop/operations",command);
                entry.set("result",result);entry.put("state","SENT");entry.remove("error");
                store.put("entry:"+entry.path("id").asText(),entry);sent++;
            } catch(ApiClient.ApiException e) {
                if(e.status()==401 || e.status()==403 || e.status()==429 || e.status()>=500)throw e;
                // A missing desktop endpoint is a deployment error, not a business rejection.
                if(e.status()!=400 && e.status()!=409 && e.status()!=422)throw e;
                entry.put("state","REJECTED");entry.put("error",e.getMessage());store.put("entry:"+entry.path("id").asText(),entry);rejected++;
            }
        }
        return sent+" opération(s) confirmée(s), "+rejected+" rejetée(s).";
    }
    /** Full replace only after all pages succeed, so deleted server rows disappear from the cache. */
    public JsonNode refresh(String parish) throws Exception {
        verifySession();ArrayNode rows=json.createArrayNode();
        for(int page=0;;page++) {
            var response=api.get("demandes/paroisse/"+UUID.fromString(parish)+"?size=100&page="+page);
            if(!response.path("content").isArray())throw new IllegalStateException("Réponse de liste invalide");
            response.path("content").forEach(rows::add);
            if(response.path("last").asBoolean())break;
            if(page>=9999)throw new IllegalStateException("Pagination trop importante");
        }
        store.put("demands:"+parish,rows);return rows;
    }
    public void catalogue(String parish) throws Exception {
        UUID.fromString(parish);
        var types=api.get("type-demandes/paroisse/"+parish);
        var catalog=json.createObjectNode();catalog.set("types",types);
        catalog.set("horaires",api.get("horaires/paroisse/"+parish));
        catalog.set("paiements",api.get("type-paiement"));
        var plans=json.createObjectNode();
        for(var type:types) {String id=type.path("publicId").asText();UUID.fromString(id);plans.set(id,api.get("forfait-tarifs/type-demande/"+id+"/actifs"));}
        catalog.set("forfaits",plans);store.put("catalogue:"+parish,catalog);
    }
    public byte[] receipt(String code,boolean online) throws Exception {
        String cache="receipt:"+code;
        if(online) {byte[] bytes=api.pdf(code);store.put(cache,json.getNodeFactory().binaryNode(bytes));return bytes;}
        var cached=store.get(cache);
        if(cached.isNull())throw new IllegalArgumentException("Ce reçu n'a pas encore été téléchargé sur ce poste.");
        return cached.binaryValue();
    }
}
