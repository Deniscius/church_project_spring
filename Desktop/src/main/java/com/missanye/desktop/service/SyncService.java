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
    private final String boundParish;
    private final java.util.function.BooleanSupplier active;
    private final ObjectMapper json=new ObjectMapper();
    public SyncService(LocalStore store,ApiClient api){this(store,api,null);}
    public SyncService(LocalStore store,ApiClient api,String parish){this(store,api,parish,()->true);}
    public SyncService(LocalStore store,ApiClient api,String parish,java.util.function.BooleanSupplier active){this.store=store;this.api=api;boundParish=parish;this.active=active;}
    public static final class SessionMismatchException extends IllegalArgumentException {
        public SessionMismatchException(){super("Reconnectez-vous avec le compte associé à cet espace de travail.");}
    }
    public JsonNode verifySession() throws Exception {
        if(!active.getAsBoolean())throw new InterruptedException("Transmission interrompue");
        var session=api.get("auth/me");
        if(!active.getAsBoolean())throw new InterruptedException("Transmission interrompue");
        if(!store.owner().equals(session.path("user").path("id").asText()))throw new SessionMismatchException();
        if(boundParish!=null&&!session.path("user").path("isGlobal").asBoolean()&&!WorkspaceBootstrap.allows(session.path("paroisses"),boundParish))throw new IllegalArgumentException("Votre accès à la paroisse de ce poste a été retiré. Reconnectez-vous après vérification des accès sur le web.");
        store.put("session",session);return session;
    }
    public String synchronize() throws Exception {
        verifySession();int sent=0,rejected=0,blocked=0;
        if(store.hasSharedJournal()){
            try{if(api.get("desktop/capabilities").path("parishJournalVersion").asInt()<1)throw sharedProtocolRequired();}
            catch(ApiClient.ApiException old){if(old.status()==404||old.status()==405)throw sharedProtocolRequired();throw old;}
        }
        for(var entry:store.entries()) {
            if(!active.getAsBoolean())throw new InterruptedException("Transmission interrompue");
            if(boundParish!=null&&!boundParish.equals(entry.path("payload").path("paroissePublicId").asText(entry.path("parish").asText())))continue;
            if(!"PENDING".equals(entry.path("state").asText()))continue;
            if(store.hasSharedJournal()&&!store.permits(LocalStore.mutationPermission(entry.path("kind").asText())))continue;
            ObjectNode payload;
            if(entry.path("preparedPayload").isObject())payload=(ObjectNode)entry.path("preparedPayload").deepCopy();
            else{
                payload=(ObjectNode)entry.path("payload").deepCopy();
                if(entry.has("dependsOn")){
                    var parent=store.get("entry:"+entry.path("dependsOn").asText());if(!"SENT".equals(parent.path("state").asText()))continue;
                    String target=parent.path("result").path("publicId").asText();if(target.isBlank())throw new IOException("La demande précédente n'est pas confirmée.");payload.put("demandePublicId",target);
                    if(!"CASH_PAYMENT".equals(entry.path("kind").asText())){String version=parent.path("result").path("updatedAt").asText();if(version.isBlank())throw new IOException("Version de demande manquante dans la confirmation du service.");payload.put("expectedUpdatedAt",version);}
                }
                entry.set("preparedPayload",payload);
            }
            var claimed=store.claimAttempt(entry,payload);if(claimed==null)continue;entry=claimed;payload=(ObjectNode)entry.path("preparedPayload");
            if(!active.getAsBoolean())throw new InterruptedException("Transmission interrompue");
            var command=json.createObjectNode().put("operationId",entry.path("id").asText()).put("kind",entry.path("kind").asText());command.set("payload",payload);
            try {
                var result=api.post("desktop/operations",command);
                if(!active.getAsBoolean())throw new InterruptedException("Transmission interrompue");
                var checked=entry.deepCopy();checked.set("payload",payload);validateConfirmation(checked,result);
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
    private IllegalArgumentException sharedProtocolRequired(){return new IllegalArgumentException("Le service doit être mis à jour avant la transmission de l'espace paroissial partagé. Votre travail est conservé.");}
    private void validateConfirmation(ObjectNode entry,JsonNode result)throws IOException{
        String kind=entry.path("kind").asText();var payload=entry.path("payload");
        if(result==null||!result.isObject()||result.path("publicId").asText().isBlank())invalidConfirmation();
        if("CREATE_DEMANDE".equals(kind)){
            if(entry.has("localSnapshot")&&(!entry.path("localReference").asText().equals(result.path("localReference").asText())||!result.path("montant").isNumber()||result.path("updatedAt").asText().isBlank()||!Set.of("EN_ATTENTE","VALIDEE","REJETEE").contains(result.path("statutValidation").asText())))invalidConfirmation();
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
        verifySession();
        JsonNode state=syncState(parish);
        var snapshot=store.demandSnapshot(parish);
        var saved=snapshot.cursor();
        JsonNode rows;
        long cursor=-1;
        if(state!=null&&saved.isIntegralNumber()&&saved.asLong()>=0&&saved.asLong()<=state.path("cursor").asLong()&&snapshot.rows().isArray()){
            cursor=saved.asLong();
            var merged=new LinkedHashMap<String,JsonNode>();
            for(var row:snapshot.rows())merged.put(row.path("publicId").asText(),row);
            for(int page=0;;page++){
                if(!active.getAsBoolean())throw new InterruptedException("Transmission interrompue");
                var delta=api.get("desktop/paroisses/"+parish+"/sync/changes?after="+cursor);
                long next=delta.path("cursor").asLong(-1);
                if(!delta.path("cursor").isIntegralNumber()||next<cursor||!delta.path("hasMore").isBoolean()||!delta.path("upserts").isArray()||!delta.path("deleted").isArray()||(delta.path("hasMore").asBoolean()&&next==cursor))throw new IOException("Synchronisation incrémentale incomplète");
                for(var row:delta.path("upserts")){
                    String id=row.path("publicId").asText();UUID.fromString(id);
                    if(!parish.equals(row.path("paroissePublicId").asText()))throw new IOException("Paroisse de synchronisation invalide");
                    merged.put(id,row);
                }
                for(var id:delta.path("deleted")){UUID.fromString(id.asText());merged.remove(id.asText());}
                cursor=next;
                if(!delta.path("hasMore").asBoolean())break;
                if(page>=9999)throw new IOException("Pagination trop importante");
            }
            var result=json.createArrayNode();merged.values().forEach(result::add);rows=result;
        }else{
            // Capture the cursor BEFORE the snapshot: concurrent writes are replayed next time.
            cursor=state==null?-1:state.path("cursor").asLong();
            rows=WorkspaceBootstrap.demands(api,parish);
        }
        if(!active.getAsBoolean())throw new InterruptedException("Transmission interrompue");
        store.writeAll(Map.of("demands:"+parish,rows,"demand-cursor:"+parish,json.getNodeFactory().numberNode(cursor)));
        if(store.workspaceReady(parish))store.selectWorkspace(parish);return rows;
    }
    public void catalogue(String parish)throws Exception{
        verifySession();var state=syncState(parish);
        String revision=state==null?"":state.path("catalogue").asText();
        var retained=store.get("catalogue:"+parish);
        if(!revision.isBlank()&&revision.equals(store.get("catalogue-revision:"+parish).asText())&&retained.path("types").isArray()&&retained.path("horaires").isArray()&&retained.path("paiements").isArray()&&retained.path("forfaits").isObject())return;
        var catalogue=WorkspaceBootstrap.catalogue(api,parish);if(!active.getAsBoolean())throw new InterruptedException("Transmission interrompue");
        var updates=new LinkedHashMap<String,JsonNode>();
        updates.put("catalogue:"+parish,catalogue);updates.put("catalogue-revision:"+parish,json.getNodeFactory().textNode(revision));
        // Demand responses contain schedule/type labels: refresh their projections once after a catalogue edit.
        if(!revision.equals(store.get("catalogue-revision:"+parish).asText()))updates.put("demand-cursor:"+parish,json.getNodeFactory().numberNode(-1));
        store.writeAll(updates);
    }
    private JsonNode syncState(String parish)throws Exception{
        UUID.fromString(parish);
        try{
            var state=api.get("desktop/paroisses/"+parish+"/sync/state");
            if(!state.path("cursor").isIntegralNumber()||state.path("cursor").asLong()<0||state.path("catalogue").asText().isBlank())throw new IOException("État de synchronisation invalide");
            return state;
        }catch(ApiClient.ApiException old){if(old.status()==404||old.status()==405)return null;throw old;}
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
