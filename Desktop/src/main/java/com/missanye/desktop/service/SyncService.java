package com.missanye.desktop.service;

import com.missanye.desktop.api.ApiClient;
import com.missanye.desktop.repository.LocalStore;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.io.IOException;
import java.util.*;

/** Runs on a serial background executor. Uncertain results retain the original UUID. */
public final class SyncService {
    private final LocalStore store;
    private final ApiClient api;
    private final ObjectMapper json=new ObjectMapper();
    public SyncService(LocalStore store,ApiClient api){this.store=store;this.api=api;}
    public static final class SessionMismatchException extends IllegalArgumentException {
        public SessionMismatchException(){super("Reconnectez-vous avec le compte associé à cet espace de travail.");}
    }
    public JsonNode verifySession() throws Exception {
        var session=api.get("auth/me");
        if(!store.owner().equals(session.path("user").path("id").asText()))throw new SessionMismatchException();
        store.put("session",session);return session;
    }
    public String synchronize() throws Exception {
        verifySession();int sent=0,rejected=0,blocked=0;
        for(var entry:store.entries()) {
            if(!"PENDING".equals(entry.path("state").asText()))continue;
            entry.put("attempts",entry.path("attempts").asInt()+1);
            store.put("entry:"+entry.path("id").asText(),entry);
            var command=json.createObjectNode().put("operationId",entry.path("id").asText()).put("kind",entry.path("kind").asText());command.set("payload",entry.path("payload"));
            try {
                var result=api.post("desktop/operations",command);
                validateConfirmation(entry,result);
                store.confirm(entry,result);sent++;
            }catch(ApiClient.ApiException e) {
                if(e.status()==401||e.status()==429||e.status()>=500)throw e;
                if(e.status()==403){entry.put("state","BLOCKED");blocked++;}
                else if(Set.of(400,409,422).contains(e.status())){entry.put("state","REJECTED");rejected++;}
                else throw e; // A missing/old desktop endpoint is not a business rejection.
                entry.put("error",e.getMessage());store.put("entry:"+entry.path("id").asText(),entry);
            }
        }
        if(sent+rejected+blocked==0)return "Aucune opération en attente de transmission.";
        return "Transmission terminée : "+sent+" confirmée(s), "+rejected+" à corriger, "+blocked+" bloquée(s).";
    }
    private void validateConfirmation(ObjectNode entry,JsonNode result)throws IOException{
        String kind=entry.path("kind").asText();var payload=entry.path("payload");
        if(result==null||!result.isObject()||result.path("publicId").asText().isBlank())invalidConfirmation();
        if("CREATE_DEMANDE".equals(kind)){
            if(result.path("codeSuivie").asText().isBlank()||!payload.path("paroissePublicId").asText().equals(result.path("paroissePublicId").asText()))invalidConfirmation();
        }else{
            if(!payload.path("demandePublicId").asText().equals(result.path("publicId").asText()))invalidConfirmation();
            if("CASH_PAYMENT".equals(kind)&&(!"PAYE".equals(result.path("statutPaiement").asText())||!result.path("montant").isNumber()||result.path("montant").decimalValue().compareTo(payload.path("expectedMontant").decimalValue())!=0))invalidConfirmation();
            if("DELETE_DEMANDE".equals(kind)&&!result.path("deleted").asBoolean())invalidConfirmation();
            if("VALIDATE_DEMANDE".equals(kind)&&!payload.path("statut").asText().equals(result.path("statutValidation").asText()))invalidConfirmation();
        }
    }
    private void invalidConfirmation()throws IOException{throw new IOException("Confirmation du service invalide : l'opération reste en attente avec son identifiant initial.");}
    /** Replace only after every page succeeds; confirmation overlays survive a failed refresh. */
    public JsonNode refresh(String parish) throws Exception {
        verifySession();var rows=WorkspaceBootstrap.demands(api,parish);store.put("demands:"+parish,rows);
        if(store.workspaceReady(parish))store.selectWorkspace(parish);return rows;
    }
    public void catalogue(String parish)throws Exception{
        verifySession();store.put("catalogue:"+parish,WorkspaceBootstrap.catalogue(api,parish));
    }
    public record ReceiptDownload(byte[] bytes,boolean cached){}
    public ReceiptDownload receiptDownload(String code,boolean online)throws Exception{
        String cache="receipt:"+code;
        if(online){
            try{byte[] bytes=api.pdf(code);store.put(cache,json.getNodeFactory().binaryNode(bytes));return new ReceiptDownload(bytes,false);}
            catch(ApiClient.ApiException e){if(e.status()<500)throw e;}
            catch(IOException e){/* A locally retained receipt can still be opened below. */}
        }
        var retained=store.get(cache);
        if(retained.isNull())throw new IllegalArgumentException("Ce reçu n'a pas encore été téléchargé sur ce poste. Une connexion est nécessaire.");
        return new ReceiptDownload(retained.binaryValue(),true);
    }
    public byte[] receipt(String code,boolean online)throws Exception{return receiptDownload(code,online).bytes();}
}
