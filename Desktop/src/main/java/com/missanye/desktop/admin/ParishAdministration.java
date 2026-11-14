package com.missanye.desktop.admin;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.missanye.desktop.api.ApiClient;
import com.missanye.desktop.repository.LocalStore;
import java.time.*;
import java.util.*;
import static com.missanye.desktop.admin.ParishModules.*;
import com.missanye.desktop.admin.ParishModules.Module;

/** Account and parish are captured per workspace. No configuration write is queued offline. */
public final class ParishAdministration {
    private static final ObjectMapper JSON=new ObjectMapper();
    private final ApiClient api;
    private final LocalStore store;
    private final String parish,owner;
    private final JsonNode session;
    public ParishAdministration(ApiClient api,LocalStore store,String parish,JsonNode session){UUID.fromString(parish);this.api=api;this.store=store;this.parish=parish;this.session=session.deepCopy();owner=session.path("user").path("id").asText();if(owner.isBlank())throw new IllegalArgumentException("Session absente.");}
    public boolean online(){return api!=null;}
    public String parish(){return parish;}
    public boolean allowed(String permission){if(permission==null)return false;for(var p:session.path("user").path("permissions"))if(permission.equals(p.asText()))return true;return false;}
    public List<Module> modules(){return all().stream().filter(m->allowed(m.read())).toList();}
    private String path(String value){return value.replace("{parish}",parish);}
    private String cache(Module module){return "administration:"+owner+":"+parish+":"+module.id();}
    private void permit(Module module,boolean writing)throws Exception{
        if(!allowed(writing?module.write():module.read()))throw new IllegalArgumentException("Vous ne disposez pas des droits nécessaires.");
        if(!session.path("user").path("isGlobal").asBoolean()&&!com.missanye.desktop.service.WorkspaceBootstrap.allows(session.path("paroisses"),parish))throw new IllegalArgumentException("Votre compte n’a pas accès à cette paroisse.");
        if(writing)verifyLivePermission(module.write());
    }
    private void verifyLivePermission(String permission)throws Exception{
        if(api==null||!allowed(permission))throw new IllegalArgumentException("Connexion et permission nécessaires pour cette action.");
        var live=api.get("auth/me");if(!owner.equals(live.path("user").path("id").asText()))throw new IllegalArgumentException("La session a changé. Reconnectez-vous.");
        boolean permitted=false;for(var p:live.path("user").path("permissions"))if(permission.equals(p.asText()))permitted=true;
        boolean assigned=live.path("user").path("isGlobal").asBoolean()||com.missanye.desktop.service.WorkspaceBootstrap.allows(live.path("paroisses"),parish);
        if(!permitted||!assigned)throw new IllegalArgumentException("Vos droits ont changé. Reconnectez-vous avant de continuer.");
    }

    public JsonNode load(Module module,LocalDate date)throws Exception{
        permit(module,false);
        if(module.id().equals("password"))return JSON.createObjectNode();
        if(api==null){var cached=store.get(cache(module)+(List.of("programme","celebrations").contains(module.id())?":"+date:""));if(cached.isNull())throw new IllegalArgumentException("Connectez-vous une première fois pour consulter cette rubrique.");return cached;}
        String suffix="";JsonNode result;
        switch(module.id()){
            case "schedules"->result=api.get("horaires/paroisse/"+parish);
            case "types"->result=api.get("type-demandes/paroisse/"+parish);
            case "prices"->{var rows=JSON.createArrayNode();for(var type:api.get("type-demandes/paroisse/"+parish)){UUID.fromString(type.path("publicId").asText());for(var price:api.get("forfait-tarifs/type-demande/"+type.path("publicId").asText()))rows.add(price);}result=rows;}
            case "choirs"->result=api.get("paroisses/"+parish+"/chorales");
            case "team"->result=api.get("admin/users/paroisse/"+parish);
            case "parish"->result=api.get("paroisses/"+parish);
            case "profile"->result=api.get("admin/profile");
            case "celebrations"->{result=api.get("dashboard/paroisses/"+parish+"/programmations/jour/"+date);suffix=":"+date;}
            case "programme"->{result=api.get("horaires/paroisse/"+parish+"/programme?debut="+date+"&fin="+date);suffix=":"+date;}
            case "archives"->{var archived=JSON.createArrayNode();for(var demand:com.missanye.desktop.service.WorkspaceBootstrap.demands(api,parish,true))if(demand.path("statusDel").asBoolean())archived.add(demand);result=archived;}
            case "invoices"->result=api.get("facture/paroisse/"+parish);
            case "payments"->result=com.missanye.desktop.service.WorkspaceBootstrap.demands(api,parish);
            case "cash"->result=api.get("details-paiement/caisse/paroisse/"+parish);
            case "account"->result=api.get("comptabilite/comptes/"+parish);
            case "transfers"->result=api.get("comptabilite/reversements/paroisse/"+parish);
            default->throw new IllegalArgumentException("Rubrique inconnue.");
        }
        store.put(cache(module)+suffix,result);return result;
    }
    public JsonNode types()throws Exception{var m=all().stream().filter(x->x.id().equals("types")).findFirst().orElseThrow();if(api!=null)return api.get("type-demandes/paroisse/"+parish);return store.get("catalogue:"+parish).path("types");}
    public ObjectNode payload(Module module,JsonNode values,JsonNode selected){
        var body=JSON.createObjectNode();for(var field:module.fields())if(values.has(field.key()))body.set(field.key(),values.get(field.key()).deepCopy());
        boolean creating=selected==null||selected.path("publicId").asText().isBlank();
        switch(module.id()){
            case "schedules"->{body.put("paroissePublicId",parish);if(body.path("dateSpecifique").asText("").isBlank()){if(body.path("uniqueSurParoisse").asBoolean())throw new IllegalArgumentException("Une messe unique nécessite une date précise.");if(body.path("jourSemaine").asText().isBlank())throw new IllegalArgumentException("Choisissez un jour ou une date ponctuelle.");body.putNull("dateSpecifique");body.put("uniqueSurParoisse",false);body.putNull("natureHonoraire");}else if(body.path("natureHonoraire").asText().isBlank())throw new IllegalArgumentException("Choisissez l’honoraire de la date ponctuelle.");}
            case "types"->{body.put("paroissePublicId",parish);int hours=body.path("delaiMinimumHeures").asInt(-1);if(hours<0||hours>8760)throw new IllegalArgumentException("Le délai doit être compris entre 0 et 8 760 heures.");requireDays(body);}
            case "prices"->{if(body.path("montantForfait").decimalValue().signum()<=0)throw new IllegalArgumentException("Le montant doit être positif.");int count=body.path("nombreCelebration").asInt();if(count<1||count>30)throw new IllegalArgumentException("Prévoyez entre 1 et 30 célébrations.");requireDays(body);if(body.path("heurePersonnalise").asBoolean()&&!body.path("natureForfait").asText().equals("SPECIALE"))throw new IllegalArgumentException("L’heure personnalisée est réservée aux honoraires spéciaux.");}
            case "team"->{if(selected!=null&&selected.path("isGlobal").asBoolean())throw new IllegalArgumentException("La gestion des comptes plateforme reste réservée à l’espace plateforme.");body.remove("roleParoisse");body.put("isGlobal",false);if(!List.of("ADMIN","SECRETAIRE","CURE","COMPTABLE_LOCAL").contains(body.path("role").asText()))throw new IllegalArgumentException("Choisissez un rôle paroissial.");if(creating){if(body.path("password").asText().length()<8)throw new IllegalArgumentException("Le mot de passe initial doit contenir au moins 8 caractères.");body.putArray("paroisses").addObject().put("paroisseId",parish).put("roleParoisse",values.path("roleParoisse").asText("SECRETAIRE"));}else{body.remove("password");for(String name:List.of("nom","prenom","username","email","telephone"))if(selected.has(name))body.set(name,selected.get(name));if(owner.equals(selected.path("publicId").asText(selected.path("id").asText()))&&!body.path("isActive").asBoolean())throw new IllegalArgumentException("Vous ne pouvez pas désactiver votre propre compte.");}}
            case "profile"->{if(selected!=null&&selected.has("email"))body.set("email",selected.get("email"));}
            case "parish"->{String rib=body.path("ibanOrRib").asText("");String bank=body.path("nomBanque").asText("");String holder=body.path("titulaireCompte").asText("");if(!rib.isBlank()||!bank.isBlank()||!holder.isBlank()){if(rib.isBlank()||bank.isBlank()||holder.isBlank())throw new IllegalArgumentException("Renseignez la banque, le titulaire et le RIB ensemble.");body.put("ibanOrRib",BankDetails.validate(rib,bank));}}
            case "password"->{if(!body.path("newPassword").asText().equals(body.path("confirmPassword").asText()))throw new IllegalArgumentException("Les deux nouveaux mots de passe sont différents.");if(body.path("newPassword").asText().length()<8)throw new IllegalArgumentException("Le nouveau mot de passe doit contenir au moins 8 caractères.");body.remove("confirmPassword");}
            case "transfers"->{if(body.path("montant").asInt()<100)throw new IllegalArgumentException("Le minimum de reversement est de 100 XOF.");body.put("paroissePublicId",parish);}
        }
        return body;
    }
    private void requireDays(JsonNode body){if(!body.path("joursCelebrationAutorises").isArray()||body.path("joursCelebrationAutorises").isEmpty())throw new IllegalArgumentException("Sélectionnez au moins un jour autorisé.");}
    public JsonNode save(Module module,JsonNode values,JsonNode selected)throws Exception{
        permit(module,true);var body=payload(module,values,selected);boolean creating=selected==null||selected.path("publicId").asText().isBlank();String endpoint,method;
        if(module.singleton()){endpoint=path(module.updatePath());method=module.id().equals("parish")?"PATCH":"PUT";}
        else if(creating){if(module.createPath()==null)throw new IllegalArgumentException("Création indisponible.");endpoint=path(module.createPath());method="POST";}
        else {if(module.updatePath()==null)throw new IllegalArgumentException("Modification indisponible.");String id=selected.path("publicId").asText();UUID.fromString(id);endpoint=path(module.updatePath())+"/"+id;method="PUT";}
        if(module.id().equals("prices")){JsonNode type=JSON.missingNode();for(var candidate:types())if(candidate.path("publicId").asText().equals(body.path("typeDemandePublicId").asText()))type=candidate;if(type.isMissingNode())throw new IllegalArgumentException("Le type de demande n’appartient pas à cette paroisse.");var allowed=new HashSet<String>();type.path("joursCelebrationAutorises").forEach(d->allowed.add(d.asText()));for(var d:body.path("joursCelebrationAutorises"))if(!allowed.contains(d.asText()))throw new IllegalArgumentException("Un jour du forfait n’est pas autorisé par le type de demande.");}
        if(module.id().equals("transfers")){var account=api.get("comptabilite/comptes/"+parish);var details=api.get("paroisses/"+parish);if(body.path("montant").decimalValue().compareTo(account.path("soldeDisponible").decimalValue())>0)throw new IllegalArgumentException("Le montant dépasse le solde disponible.");if(details.path("ibanOrRib").asText("").isBlank())throw new IllegalArgumentException("Renseignez le RIB de la paroisse avant de demander un reversement.");}
        return api.send(method,endpoint,body);
    }
    public void remove(Module module,JsonNode selected)throws Exception{permit(module,true);if(!module.removable())throw new IllegalArgumentException("Suppression indisponible.");String id=selected.path("publicId").asText();UUID.fromString(id);api.send("DELETE",path(module.updatePath())+"/"+id,null);}
    public JsonNode programme(LocalDate date,ArrayNode slots,boolean reset)throws Exception{var module=all().stream().filter(m->m.id().equals("programme")).findFirst().orElseThrow();permit(module,true);String endpoint="horaires/paroisse/"+parish+"/programme/"+date;if(reset)return api.send("DELETE",endpoint+"/personnalisation",null);return api.send("PUT",endpoint,JSON.createObjectNode().set("creneaux",slots));}
    public JsonNode daySlots(LocalDate date)throws Exception{if(api==null)return JSON.createArrayNode();var days=api.get("horaires/paroisse/"+parish+"/programme?debut="+date+"&fin="+date);return days.path(0).path("creneaux");}
    public JsonNode moveCelebration(JsonNode celebration,String schedule)throws Exception{
        var m=all().stream().filter(x->x.id().equals("celebrations")).findFirst().orElseThrow();permit(m,true);String id=celebration.path("demandeDatePublicId").asText();UUID.fromString(id);UUID.fromString(schedule);return api.send("PATCH","dashboard/paroisses/"+parish+"/programmations/"+id+"/horaire",JSON.createObjectNode().put("horairePublicId",schedule));
    }
    public JsonNode markCelebrated(JsonNode celebration)throws Exception{
        verifyLivePermission("celebration:manage");String id=celebration.path("demandeDatePublicId").asText();UUID.fromString(id);return api.post("celebrations/dates/"+id+"/marquer-celebree",JSON.createObjectNode());
    }
    public JsonNode invite(JsonNode choir)throws Exception{verifyLivePermission("choir:invite");String id=choir.path("publicId").asText();UUID.fromString(id);return api.post("chorales/"+id+"/invitations",JSON.createObjectNode());}
    public JsonNode logo(java.nio.file.Path image)throws Exception{var m=all().stream().filter(x->x.id().equals("parish")).findFirst().orElseThrow();permit(m,true);return image==null?api.send("DELETE","paroisses/"+parish+"/logo",null):api.uploadLogo(parish,image);}
    public byte[] sampleReceipt()throws Exception{if(!allowed("parish:read")||api==null)throw new IllegalArgumentException("Connexion et permission de lecture de la paroisse nécessaires.");return api.binary("paroisses/"+parish+"/recu-modele.pdf");}
}
