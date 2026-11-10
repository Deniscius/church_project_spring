package com.missanye.desktop.service;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.missanye.desktop.model.RequestValidation;
import com.missanye.desktop.repository.LocalStore;
import java.time.*;
import java.util.*;

/** Workstation business facts remain distinct from server confirmation. All methods run on the I/O executor. */
public final class LocalRequestService {
    private static final ObjectMapper JSON=new ObjectMapper();private final LocalStore store;
    public LocalRequestService(LocalStore store){this.store=store;}
    private void permission(String name)throws Exception{
        var session=store.get("session");if(!store.owner().equals(session.path("user").path("id").asText()))throw new IllegalArgumentException("Session locale invalide.");
        for(var p:session.path("user").path("permissions"))if(name.equals(p.asText()))return;
        throw new IllegalArgumentException("Vous ne disposez pas des droits nécessaires pour cette action.");
    }
    private ObjectNode entry(String id)throws Exception{var node=store.get("entry:"+id);if(!(node instanceof ObjectNode object))throw new IllegalArgumentException("Demande locale introuvable.");return object;}
    public static String reference(String id){return "MSL-"+UUID.fromString(id).toString().toUpperCase(Locale.ROOT);}
    public ObjectNode finalizeDraft(String id)throws Exception{return finalizeEntry(entry(id));}
    public ObjectNode finalizePayload(String id,JsonNode payload)throws Exception{
        var candidate=id==null?JSON.createObjectNode().put("id",UUID.randomUUID().toString()).put("kind","CREATE_DEMANDE").put("state","DRAFT").put("createdAt",Instant.now().toString()):entry(id);
        candidate.set("payload",payload.deepCopy());return finalizeEntry(candidate);
    }
    private ObjectNode finalizeEntry(ObjectNode entry)throws Exception{
        permission("demand:edit");String id=entry.path("id").asText();if(!"DRAFT".equals(entry.path("state").asText()))throw new IllegalArgumentException("Seul un brouillon peut être finalisé.");
        var payload=RequestValidation.forSubmission(entry.path("payload"));String parish=payload.path("paroissePublicId").asText();
        if(!WorkspaceBootstrap.allows(store.get("parishes"),parish))throw new IllegalArgumentException("Cette paroisse n'est pas autorisée.");
        var catalogue=store.get("catalogue:"+parish);JsonNode plan=null;
        for(var candidate:catalogue.path("forfaits").path(payload.path("typeDemandePublicId").asText()))if(payload.path("forfaitTarifPublicId").asText().equals(candidate.path("publicId").asText())&&candidate.path("isActive").asBoolean(true)&&!candidate.path("statusDel").asBoolean())plan=candidate;
        if(plan==null||!plan.path("montantForfait").isNumber()||plan.path("montantForfait").decimalValue().signum()<=0)throw new IllegalArgumentException("Le forfait et son montant doivent avoir été chargés sur ce poste.");
        int amount;try{amount=plan.path("montantForfait").decimalValue().intValueExact();}catch(Exception invalid){throw new IllegalArgumentException("Montant de forfait invalide.");}
        int days=payload.path("datesCelebration").isEmpty()?1:payload.path("datesCelebration").size();if(days!=plan.path("nombreCelebration").asInt(1))throw new IllegalArgumentException("Les dates ne correspondent pas au forfait.");
        if(LocalDate.parse(payload.path("dateDebut").asText()).isBefore(LocalDate.now()))throw new IllegalArgumentException("La première célébration est déjà passée.");
        if(!Set.of("NORMALE","SPECIALE").contains(plan.path("natureForfait").asText()))throw new IllegalArgumentException("La nature du forfait doit être actualisée sur ce poste.");
        boolean special="SPECIALE".equals(plan.path("natureForfait").asText());if(special&&payload.path("emailFidele").asText().isBlank())throw new IllegalArgumentException("Une adresse e-mail est nécessaire pour une demande spéciale.");
        var view=payload.deepCopy();view.put("publicId","local:"+id).put("localRequestId",id).put("localReference",reference(id)).put("codeSuivie",reference(id)).put("montant",amount).put("validationRequise",special).put("statutValidation",special?"EN_ATTENTE":"VALIDEE").put("statutPaiement","NON_PAYE").put("paiementDisponible",!special).put("localPending",true);
        view.put("recordedAt",Instant.now().toString());entry.set("payload",payload);entry.set("localSnapshot",view);entry.put("localRequestId",id).put("localReference",reference(id)).put("codeSuivie",reference(id)).put("parish",parish).put("state","PENDING");store.put("entry:"+id,entry);return view;
    }
    private List<ObjectNode> related(String id)throws Exception{
        var result=new ArrayList<ObjectNode>();String serverId=entry(id).path("result").path("publicId").asText();
        for(var e:store.entries())if(!id.equals(e.path("id").asText())&&(id.equals(e.path("localRequestId").asText())||(!serverId.isBlank()&&serverId.equals(e.path("payload").path("demandePublicId").asText()))))result.add(e);return result;
    }
    public ObjectNode view(String id)throws Exception{
        var root=entry(id);if(!WorkspaceBootstrap.allows(store.get("parishes"),root.path("parish").asText()))throw new IllegalArgumentException("Cette paroisse n’est pas autorisée pour ce compte.");if(!(root.get("localSnapshot") instanceof ObjectNode))throw new IllegalArgumentException("La demande doit d'abord être finalisée.");
        return project(root,related(id),store.get("demands:"+root.path("parish").asText()));
    }
    private ObjectNode project(ObjectNode root,List<ObjectNode> relations,JsonNode remote){
        String id=root.path("id").asText();
        ObjectNode view=root.path("result").isObject()?((ObjectNode)root.path("result")).deepCopy():((ObjectNode)root.path("localSnapshot")).deepCopy();
        boolean cached=false;String serverId=root.path("result").path("publicId").asText();if(!serverId.isBlank())for(var row:remote)if(serverId.equals(row.path("publicId").asText())){view=(ObjectNode)row.deepCopy();cached=true;break;}
        boolean review=Set.of("REJECTED","BLOCKED","SUPERSEDED").contains(root.path("state").asText());boolean pending=!"SENT".equals(root.path("state").asText());String localValidation=view.path("statutValidation").asText();boolean localCash=false;String cashId="";boolean decisionRecorded=false;
        for(var e:relations){
            boolean sent="SENT".equals(e.path("state").asText());pending|=!sent;review|=Set.of("REJECTED","BLOCKED").contains(e.path("state").asText());
            if("VALIDATE_DEMANDE".equals(e.path("kind").asText())){if(!sent){decisionRecorded=true;localValidation=e.path("payload").path("statut").asText();}if(sent&&!cached&&e.path("result").isObject()){view=(ObjectNode)e.path("result").deepCopy();localValidation=view.path("statutValidation").asText();}}
            if("CASH_PAYMENT".equals(e.path("kind").asText())){localCash=true;cashId=e.path("id").asText();if(sent&&!cached&&e.path("result").isObject())view=(ObjectNode)e.path("result").deepCopy();}
        }
        int quoted=root.path("localSnapshot").path("montant").asInt();String cashRecorded="";for(var e:relations)if(cashId.equals(e.path("id").asText())){quoted=e.path("payload").path("expectedMontant").asInt(quoted);cashRecorded=e.path("createdAt").asText();}
        view.put("localCashRecordedAt",cashRecorded);
        view.put("localDecisionRecorded",decisionRecorded);
        view.put("localQuotedMontant",quoted).put("localRecordedAt",root.path("localSnapshot").path("recordedAt").asText()).put("localReconciliationRequired",review);
        view.put("localRequestId",id).put("localReference",reference(id)).put("localPending",pending).put("localCashReceived",localCash).put("localCashId",cashId).put("localValidation",localValidation);
        if(!view.has("codeSuivie"))view.put("codeSuivie",reference(id));
        // Local decisions and cash are visible without claiming central confirmation.
        view.put("statutValidation",localValidation);if(localCash&&!"PAYE".equals(view.path("statutPaiement").asText()))view.put("statutPaiement","ESPECES_ENREGISTREES");
        view.put("paiementDisponible",!review&&"VALIDEE".equals(localValidation)&&!localCash&&!"PAYE".equals(view.path("statutPaiement").asText()));return view;
    }
    public ArrayNode overlay(String parish,JsonNode remote)throws Exception{
        var rows=JSON.createArrayNode();var replacements=new LinkedHashMap<String,JsonNode>();var local=new ArrayList<JsonNode>();var journal=store.entries();
        if(!WorkspaceBootstrap.allows(store.get("parishes"),parish))throw new IllegalArgumentException("Cette paroisse n'est pas autorisée.");
        for(var root:journal)if(root.has("localSnapshot")&&parish.equals(root.path("parish").asText())&&!Set.of("ARCHIVED","SUPERSEDED").contains(root.path("state").asText())){
            String requestId=root.path("id").asText(),serverId=root.path("result").path("publicId").asText();
            var relations=journal.stream().filter(e->!requestId.equals(e.path("id").asText())&&(requestId.equals(e.path("localRequestId").asText())||(!serverId.isBlank()&&serverId.equals(e.path("payload").path("demandePublicId").asText())))).toList();
            var view=project(root,relations,remote);if(root.path("result").path("publicId").asText().isBlank())local.add(view);else replacements.put(root.path("result").path("publicId").asText(),view);
        }
        for(var row:remote){String id=row.path("publicId").asText();var replacement=replacements.remove(id);if(replacement!=null){
            // Once all commands are confirmed, the fresh central row wins over historical responses.
            if(!replacement.path("localPending").asBoolean()){var fresh=(ObjectNode)row.deepCopy();fresh.put("localRequestId",replacement.path("localRequestId").asText()).put("localReference",replacement.path("localReference").asText()).put("localPending",false);rows.add(fresh);}else rows.add(replacement);
        }else rows.add(row.deepCopy());}
        for(var pending:replacements.values())if(pending.path("localPending").asBoolean())rows.add(pending);local.forEach(rows::add);
        for(var operation:journal)if(!operation.has("localRequestId")&&Set.of("PENDING","BLOCKED","REJECTED").contains(operation.path("state").asText())){
            String target=operation.path("payload").path("demandePublicId").asText();for(var node:rows)if(!target.isBlank()&&target.equals(node.path("publicId").asText())){
                var row=(ObjectNode)node;row.put("localPending",true);if(!"PENDING".equals(operation.path("state").asText())){row.put("localReconciliationRequired",true);row.put("paiementDisponible",false);}
                if("VALIDATE_DEMANDE".equals(operation.path("kind").asText())){row.put("localDecisionRecorded",true);row.put("localValidation",operation.path("payload").path("statut").asText());if("PENDING".equals(operation.path("state").asText())){row.put("statutValidation",operation.path("payload").path("statut").asText());row.put("paiementDisponible","VALIDEE".equals(row.path("statutValidation").asText()));}}
                if("CASH_PAYMENT".equals(operation.path("kind").asText())){row.put("localCashReceived",true).put("localCashRecordedAt",operation.path("createdAt").asText()).put("localCashId",operation.path("id").asText()).put("localQuotedMontant",operation.path("payload").path("expectedMontant").asInt()).put("statutPaiement","ESPECES_ENREGISTREES").put("paiementDisponible",false);}
            }
        }return rows;
    }
    public ObjectNode decide(String id,String decision)throws Exception{
        permission("demand:validate");if(!Set.of("VALIDEE","REJETEE").contains(decision))throw new IllegalArgumentException("Décision invalide.");var root=entry(id);var view=view(id);if(view.path("localReconciliationRequired").asBoolean())throw new IllegalArgumentException("Rapprochez d’abord le retour du service pour cette demande.");
        if(!view.path("validationRequise").asBoolean())throw new IllegalArgumentException("Cette demande ne nécessite pas de validation manuelle.");
        if(view.path("localCashReceived").asBoolean())throw new IllegalArgumentException("Un encaissement a déjà été enregistré pour cette demande.");
        var previous=related(id);if(!previous.isEmpty())throw new IllegalArgumentException("Une décision a déjà été enregistrée ; elle doit être rapprochée avant une nouvelle décision.");
        return command(root,"VALIDATE_DEMANDE",JSON.createObjectNode().put("statut",decision),id);
    }
    public ObjectNode receiveCash(String id)throws Exception{
        permission("payment:manage");var root=entry(id);var view=view(id);
        if(!view.path("paiementDisponible").asBoolean())throw new IllegalArgumentException("La demande doit être validée et ne pas avoir d'encaissement avant cette action.");
        String parent=id;for(var e:related(id)){if("CASH_PAYMENT".equals(e.path("kind").asText()))throw new IllegalArgumentException("Un encaissement existe déjà pour cette demande.");if("VALIDATE_DEMANDE".equals(e.path("kind").asText()))parent=e.path("id").asText();}
        return command(root,"CASH_PAYMENT",JSON.createObjectNode().put("expectedMontant",root.path("localSnapshot").path("montant").asInt()),parent);
    }
    public byte[] receiptFor(JsonNode demand)throws Exception{
        if(demand.has("localRequestId"))return receipt(demand.path("localRequestId").asText());
        receiptPermission();if(!WorkspaceBootstrap.allows(store.get("parishes"),demand.path("paroissePublicId").asText()))throw new IllegalArgumentException("Cette paroisse n’est pas autorisée pour ce compte.");var view=(ObjectNode)demand.deepCopy();String id=demand.path("publicId").asText();UUID.fromString(id);
        view.put("localReference",demand.path("codeSuivie").asText()).put("localRequestId",id).put("localQuotedMontant",demand.path("localQuotedMontant").asInt(demand.path("montant").asInt())).put("localRecordedAt",demand.path("updatedAt").asText(demand.path("createdAt").asText())).put("localValidation",demand.path("statutValidation").asText());
        String name="Paroisse";for(var p:store.get("parishes"))if(demand.path("paroissePublicId").asText().equals(WorkspaceBootstrap.parishId(p)))name=p.path("nom").asText("Paroisse");
        return retainReceipt("server-"+id,name,view);
    }
    private void receiptPermission()throws Exception{
        var permissions=store.get("session").path("user").path("permissions");boolean allowed=false;for(var p:permissions)if(Set.of("invoice:read","receipt:manage").contains(p.asText()))allowed=true;
        if(!allowed||!store.owner().equals(store.get("session").path("user").path("id").asText()))throw new IllegalArgumentException("Vous ne disposez pas des droits pour éditer un reçu.");
    }
    public byte[] receipt(String id)throws Exception{
        receiptPermission();
        var view=view(id);String parish=view.path("paroissePublicId").asText(),name="Paroisse";for(var p:store.get("parishes"))if(parish.equals(WorkspaceBootstrap.parishId(p)))name=p.path("nom").asText("Paroisse");
        return retainReceipt(id,name,view);
    }
    private byte[] retainReceipt(String id,String name,ObjectNode view)throws Exception{
        String fingerprint=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(JSON.writeValueAsBytes(view)));String key="local-receipt:"+id+":"+fingerprint;
        var retained=store.get(key);if(!retained.isNull())return retained.binaryValue();byte[] pdf=LocalReceipt.render(name,view);store.put(key,JSON.getNodeFactory().binaryNode(pdf));return pdf;
    }
    private ObjectNode command(ObjectNode root,String kind,ObjectNode payload,String parent)throws Exception{
        String id=UUID.randomUUID().toString();var command=JSON.createObjectNode().put("id",id).put("kind",kind).put("state","PENDING").put("createdAt",Instant.now().toString()).put("localRequestId",root.path("id").asText()).put("dependsOn",parent).put("parish",root.path("parish").asText()).put("codeSuivie",root.path("localReference").asText());command.set("payload",payload);store.put("entry:"+id,command);return command;
    }
}
