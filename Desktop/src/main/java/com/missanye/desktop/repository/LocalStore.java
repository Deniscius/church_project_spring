package com.missanye.desktop.repository;

import com.fasterxml.jackson.databind.*;
import com.missanye.desktop.model.RequestValidation;
import com.missanye.desktop.service.WorkspaceBootstrap;
import com.fasterxml.jackson.databind.node.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/** Private account identity/cache with an optional encrypted parish journal. No session credential is stored. */
public final class LocalStore implements AutoCloseable {
    private static final ObjectMapper JSON=new ObjectMapper();
    private final String url, scope;
    private final Path file;
    private final String server;
    private LocalStore parishJournal;
    private String journalParish;
    private byte[] key;
    private String owner;
    private LocalStore(Path path, String server, String username) throws Exception {
        file=path;this.server=server;
        Files.createDirectories(path.toAbsolutePath().getParent());
        url="jdbc:sqlite:"+path.toAbsolutePath();
        String exact=scope(server,username.trim());String canonical=scope(server,username.trim().toLowerCase(Locale.ROOT));
        try(var c=connect(); var s=c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS vault_profile(scope TEXT PRIMARY KEY,salt BLOB NOT NULL,proof BLOB NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS vault_data(scope TEXT NOT NULL,name TEXT NOT NULL,value BLOB NOT NULL,PRIMARY KEY(scope,name))");
            scope=existingScope(c,exact,canonical);
        }
    }
    public enum ProfileStatus {ABSENT,NEEDS_ONLINE,READY}
    private static String scope(String server,String username)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((server+"\n"+username).getBytes(StandardCharsets.UTF_8)));}
    private static String existingScope(Connection c,String exact,String canonical)throws SQLException{
        try(var query=c.prepareStatement("SELECT scope FROM vault_profile WHERE scope=?")){query.setString(1,exact);try(var rows=query.executeQuery()){return rows.next()?exact:canonical;}}
    }
    public static ProfileStatus inspect(Path file,String server,String username)throws Exception{
        if(username==null||username.isBlank()||!Files.isRegularFile(file))return ProfileStatus.ABSENT;
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+file.toAbsolutePath())){
            try(var table=c.createStatement();var names=table.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='vault_profile'")){if(!names.next())return ProfileStatus.ABSENT;}
            String chosen=existingScope(c,scope(server,username.trim()),scope(server,username.trim().toLowerCase(Locale.ROOT)));
            try(var exists=c.prepareStatement("SELECT 1 FROM vault_profile WHERE scope=?")){exists.setString(1,chosen);try(var rows=exists.executeQuery()){if(!rows.next())return ProfileStatus.ABSENT;}}
            var catalogues=new HashSet<String>();var demands=new HashSet<String>();
            try(var q=c.prepareStatement("SELECT name FROM vault_data WHERE scope=?")){q.setString(1,chosen);try(var rows=q.executeQuery()){while(rows.next()){String name=rows.getString(1);if(name.startsWith("catalogue:"))catalogues.add(name.substring(10));if(name.startsWith("demands:"))demands.add(name.substring(8));}}}
            catalogues.retainAll(demands);return catalogues.isEmpty()?ProfileStatus.NEEDS_ONLINE:ProfileStatus.READY;
        }
    }
    private Connection connect() throws SQLException {
        var c=DriverManager.getConnection(url);
        try(var s=c.createStatement()) {s.execute("PRAGMA busy_timeout=5000");}
        return c;
    }
    public static LocalStore unlock(Path file,String server,String username,char[] password,String onlineOwner) throws Exception {
        var store=new LocalStore(file,server,username);
        try(var c=store.connect(); var s=c.prepareStatement("SELECT salt,proof FROM vault_profile WHERE scope=?")) {
            s.setString(1,store.scope);
            try(var r=s.executeQuery()) {
                if(r.next()) {
                    store.key=derive(password,r.getBytes(1));
                    try {store.owner=new String(store.decrypt("proof",r.getBytes(2)),StandardCharsets.UTF_8);}
                    catch(GeneralSecurityException e) {throw new IllegalArgumentException("Mot de passe local incorrect ou coffre endommagé.");}
                    if(onlineOwner!=null && !onlineOwner.equals(store.owner)) throw new IllegalArgumentException("Ce coffre appartient à un autre compte serveur.");
                } else {
                    if(onlineOwner==null) throw new IllegalArgumentException("Première connexion en ligne requise pour ce compte.");
                    if(password.length<12) throw new IllegalArgumentException("Choisissez un mot de passe local d'au moins 12 caractères.");
                    byte[] salt=new byte[32]; new SecureRandom().nextBytes(salt);
                    store.key=derive(password,salt); store.owner=onlineOwner;
                    try(var insert=c.prepareStatement("INSERT INTO vault_profile VALUES(?,?,?)")) {
                        insert.setString(1,store.scope);insert.setBytes(2,salt);insert.setBytes(3,store.encrypt("proof",onlineOwner.getBytes(StandardCharsets.UTF_8)));insert.executeUpdate();
                    }
                }
            }
        } catch(Exception e) {store.close();throw e;}
        finally {Arrays.fill(password,'\0');}
        return store;
    }
    private static byte[] derive(char[] password,byte[] salt) throws Exception {
        var spec=new PBEKeySpec(password,salt,210000,256);
        try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}
        finally{spec.clearPassword();}
    }
    private byte[] encrypt(String name,byte[] bytes) throws GeneralSecurityException {
        if(key==null) throw new IllegalStateException("Coffre verrouillé");
        byte[] iv=new byte[12];new SecureRandom().nextBytes(iv);
        var cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,iv));
        cipher.updateAAD((scope+":"+name).getBytes(StandardCharsets.UTF_8));byte[] encrypted=cipher.doFinal(bytes);
        return ByteBuffer.allocate(12+encrypted.length).put(iv).put(encrypted).array();
    }
    private byte[] decrypt(String name,byte[] bytes) throws GeneralSecurityException {
        if(key==null) throw new IllegalStateException("Coffre verrouillé");
        if(bytes.length<28) throw new GeneralSecurityException("Données invalides");
        var cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,Arrays.copyOf(bytes,12)));
        cipher.updateAAD((scope+":"+name).getBytes(StandardCharsets.UTF_8));return cipher.doFinal(bytes,12,bytes.length-12);
    }
    public boolean hasSharedJournal(){return parishJournal!=null;}
    public static String mutationPermission(String kind){return switch(kind){case "CREATE_DEMANDE","UPDATE_INTENTION"->"demand:edit";case "VALIDATE_DEMANDE"->"demand:validate";case "CASH_PAYMENT"->"payment:manage";case "DELETE_DEMANDE"->"demand:delete";default->throw new IllegalArgumentException("Opération inconnue");};}
    public synchronized boolean permits(String permission)throws Exception{for(var p:get("session").path("user").path("permissions"))if(permission.equals(p.asText()))return true;return false;}
    private void sharedAccess(String parish)throws Exception{
        if(!owner.equals(get("session").path("user").path("id").asText())||!WorkspaceBootstrap.allows(get("parishes"),parish)||!permits("demand:read"))throw new IllegalArgumentException("Vous n'avez pas accès à cet espace paroissial.");
    }
    private boolean sharedName(String name){return parishJournal!=null&&(name.startsWith("entry:")||name.startsWith("local-receipt:")||name.equals("catalogue:"+journalParish)||name.equals("demands:"+journalParish));}
    private String entryParish(JsonNode entry){return entry.path("parish").asText(entry.path("payload").path("paroissePublicId").asText(entry.path("localSnapshot").path("paroissePublicId").asText()));}
    private ObjectNode auditEntry(ObjectNode entry,JsonNode previous)throws Exception{
        var value=entry.deepCopy();var user=get("session").path("user");
        if(previous.isObject()&&previous.has("createdBy")){
            for(String field:List.of("createdBy","createdByName","createdByRole"))value.set(field,previous.path(field));
        }else{value.put("createdBy",owner).put("createdByName",user.path("username").asText()).put("createdByRole",user.path("role").asText());}
        if("DRAFT".equals(previous.path("state").asText())&&"PENDING".equals(value.path("state").asText()))value.put("finalizedBy",owner);
        value.put("updatedBy",owner).put("localUpdatedAt",Instant.now().toString());return value;
    }
    /** Called only after account identity and parish membership are checked. Old account copies remain as backups. */
    public synchronized void attachParishJournal(String parish,char[] password,boolean freshData)throws Exception{
        LocalStore shared=null;
        try{
            sharedAccess(parish);if(parishJournal!=null)throw new IllegalStateException("L'espace paroissial est déjà ouvert.");
            shared=unlock(file,server,"@parish:"+parish,password,"parish:"+parish);
            var imports=new LinkedHashMap<String,JsonNode>();
            for(var entry:entries())if(parish.equals(entryParish(entry))&&shared.get("entry:"+entry.path("id").asText()).isNull())imports.put("entry:"+entry.path("id").asText(),auditEntry(entry,JSON.nullNode()).put("legacyImported",true).put("createdByRole",""));
            for(String prefix:List.of("catalogue:","demands:")){String name=prefix+parish;var value=get(name);if(!value.isNull()&&(freshData||shared.get(name).isNull()))imports.put(name,value);}
            if(!imports.isEmpty())shared.writeAll(imports);
            journalParish=parish;parishJournal=shared;
        }catch(Exception failure){if(shared!=null)shared.close();throw failure;}finally{Arrays.fill(password,'\0');}
    }
    public String owner(){return owner;}
    public synchronized void saveSession(JsonNode session,JsonNode parishes)throws Exception{
        if(!owner.equals(session.path("user").path("id").asText()))throw new IllegalArgumentException("Ce compte ne correspond pas à cet espace de travail.");
        writeAll(Map.of("session",session,"parishes",parishes));
    }
    private boolean completeCatalogue(JsonNode c){return c.isObject()&&c.path("types").isArray()&&c.path("horaires").isArray()&&c.path("paiements").isArray()&&c.path("forfaits").isObject();}
    public synchronized boolean workspaceReady(String parish)throws Exception{
        var session=get("session");return owner.equals(session.path("user").path("id").asText())&&WorkspaceBootstrap.allows(get("parishes"),parish)&&completeCatalogue(get("catalogue:"+parish))&&get("demands:"+parish).isArray();
    }
    public synchronized void installWorkspace(JsonNode session,JsonNode parishes,String parish,WorkspaceBootstrap.Prepared prepared)throws Exception{
        if(!owner.equals(session.path("user").path("id").asText())||!owner.equals(prepared.session().path("user").path("id").asText())||!WorkspaceBootstrap.allows(parishes,parish))throw new IllegalArgumentException("Cet espace n'est pas autorisé pour le compte connecté.");
        if(!completeCatalogue(prepared.catalogue())||!prepared.demands().isArray())throw new IllegalArgumentException("La préparation de la paroisse est incomplète. Réessayez en ligne.");
        var marker=JSON.createObjectNode().put("version",1).put("owner",owner).put("parish",parish).put("preparedAt",Instant.now().toString());
        writeAll(Map.of("session",prepared.session(),"parishes",parishes,"catalogue:"+parish,prepared.catalogue(),"demands:"+parish,prepared.demands(),"ready:"+parish,marker,"selected-parish",JSON.getNodeFactory().textNode(parish)));
    }
    public synchronized void selectWorkspace(String parish)throws Exception{
        if(!workspaceReady(parish))throw new IllegalArgumentException("Une connexion est nécessaire pour préparer cette paroisse sur ce poste.");
        put("selected-parish",JSON.getNodeFactory().textNode(parish));
    }
    private boolean cashEvidence(String id)throws Exception{
        for(var e:entries())if("CASH_PAYMENT".equals(e.path("kind").asText())&&(id.equals(e.path("id").asText())||id.equals(e.path("localRequestId").asText())||id.equals(e.path("dependsOn").asText())))return true;return false;
    }
    public synchronized ObjectNode correctDraft(String rejectedId,JsonNode payload)throws Exception{
        if(cashEvidence(rejectedId))throw new IllegalArgumentException("Des espèces ont été enregistrées. Rapprochez cet encaissement avant de remplacer la demande.");
        var original=(ObjectNode)get("entry:"+rejectedId);
        if(!"REJECTED".equals(original.path("state").asText())||!"CREATE_DEMANDE".equals(original.path("kind").asText()))throw new IllegalArgumentException("Cette demande a déjà été corrigée ou ne peut pas être modifiée.");
        var corrected=JSON.createObjectNode().put("id",UUID.randomUUID().toString()).put("createdAt",Instant.now().toString()).put("kind","CREATE_DEMANDE").put("state","DRAFT").put("correctedFrom",rejectedId);corrected.set("payload",payload);
        original.put("state","SUPERSEDED").put("replacedBy",corrected.path("id").asText());
        writeAll(Map.of("entry:"+rejectedId,original,"entry:"+corrected.path("id").asText(),corrected));return corrected;
    }

    public synchronized void put(String name,JsonNode value) throws Exception {writeAll(Map.of(name,value));}
    public synchronized void writeAll(Map<String,? extends JsonNode> values) throws Exception {
        if(parishJournal!=null){
            var accountValues=new LinkedHashMap<String,JsonNode>();var parishValues=new LinkedHashMap<String,JsonNode>();
            for(var item:values.entrySet()){
                if(!sharedName(item.getKey())){accountValues.put(item.getKey(),item.getValue());continue;}
                sharedAccess(journalParish);JsonNode value=item.getValue();
                if(item.getKey().startsWith("entry:")){
                    if(!journalParish.equals(entryParish(value)))throw new IllegalArgumentException("Cette opération appartient à une autre paroisse.");
                    if(!permits(mutationPermission(value.path("kind").asText())))throw new IllegalArgumentException("Vos droits ne permettent pas cette action.");
                    var previous=parishJournal.get(item.getKey());
                    if(previous.isObject()&&!previous.path("kind").equals(value.path("kind")))throw new IllegalArgumentException("Le type de cette opération ne peut pas être modifié.");
                    if(previous.isObject()&&!"DRAFT".equals(previous.path("state").asText())&&(!previous.path("payload").equals(value.path("payload"))||!previous.path("localSnapshot").equals(value.path("localSnapshot"))))throw new IllegalArgumentException("Une opération finalisée ne peut pas être remplacée.");
                    value=auditEntry((ObjectNode)value,previous);
                }
                if(item.getKey().startsWith("local-receipt:")&&!permits("invoice:read")&&!permits("receipt:manage"))throw new IllegalArgumentException("Vous ne pouvez pas éditer un reçu.");
                parishValues.put(item.getKey(),value);
            }
            if(!parishValues.isEmpty())parishJournal.writeAll(parishValues);
            if(accountValues.isEmpty())return;
            values=accountValues;
        }
        var encoded=new LinkedHashMap<String,byte[]>();
        for(var value:values.entrySet())encoded.put(value.getKey(),encrypt(value.getKey(),JSON.writeValueAsBytes(value.getValue())));
        try(var c=connect()) {
            c.setAutoCommit(false);
            try(var s=c.prepareStatement("INSERT INTO vault_data VALUES(?,?,?) ON CONFLICT(scope,name) DO UPDATE SET value=excluded.value")) {
                for(var value:encoded.entrySet()){s.setString(1,scope);s.setString(2,value.getKey());s.setBytes(3,value.getValue());s.addBatch();}
                s.executeBatch();c.commit();
            }catch(Exception e){c.rollback();throw e;}
        }
    }
    /** Claim and freeze an attempt atomically against a user's return-to-draft action. */
    public synchronized ObjectNode claimAttempt(ObjectNode expected,ObjectNode payload)throws Exception{
        var actual=get("entry:"+expected.path("id").asText());
        if(!(actual instanceof ObjectNode current)||!"PENDING".equals(current.path("state").asText())||!current.path("payload").equals(expected.path("payload"))||!current.path("kind").equals(expected.path("kind")))return null;
        if(!current.path("preparedPayload").isObject())current.set("preparedPayload",payload.deepCopy());
        current.put("attempts",current.path("attempts").asInt()+1);put("entry:"+current.path("id").asText(),current);return current;
    }
    public synchronized void confirm(ObjectNode entry,JsonNode result) throws Exception {
        var confirmed=entry.deepCopy();confirmed.set("result",result);confirmed.put("state","SENT");confirmed.remove("error");
        var updates=new LinkedHashMap<String,JsonNode>();updates.put("entry:"+entry.path("id").asText(),confirmed);
        String parish=result.path("paroissePublicId").asText(entry.path("parish").asText(entry.path("payload").path("paroissePublicId").asText()));
        String id=result.path("publicId").asText();
        if(!parish.isBlank()&&!id.isBlank()&&(result.has("intention")||result.path("deleted").asBoolean())) {
            var rows=JSON.createArrayNode();boolean found=false;
            for(var row:get("demands:"+parish)) {
                if(id.equals(row.path("publicId").asText())){found=true;if(!result.path("deleted").asBoolean())rows.add(result);}
                else rows.add(row);
            }
            if(!found&&!result.path("deleted").asBoolean())rows.add(result);
            updates.put("demands:"+parish,rows);
        }
        writeAll(updates);
    }
    public synchronized JsonNode get(String name) throws Exception {
        if(sharedName(name)){sharedAccess(journalParish);if(name.startsWith("local-receipt:")&&!permits("invoice:read")&&!permits("receipt:manage"))throw new IllegalArgumentException("Vous ne pouvez pas consulter ce reçu.");return parishJournal.get(name);}
        try(var c=connect();var s=c.prepareStatement("SELECT value FROM vault_data WHERE scope=? AND name=?")) {
            s.setString(1,scope);s.setString(2,name);try(var r=s.executeQuery()){return r.next()?JSON.readTree(decrypt(name,r.getBytes(1))):JSON.nullNode();}
        }
    }
    public synchronized List<ObjectNode> entries() throws Exception {
        if(parishJournal!=null){sharedAccess(journalParish);return parishJournal.entries();}
        var result=new ArrayList<ObjectNode>();
        try(var c=connect();var s=c.prepareStatement("SELECT name,value FROM vault_data WHERE scope=? AND name LIKE 'entry:%'")) {
            s.setString(1,scope);try(var r=s.executeQuery()){while(r.next())result.add((ObjectNode)JSON.readTree(decrypt(r.getString(1),r.getBytes(2))));}
        }
        result.sort(Comparator.comparing(e -> e.path("createdAt").asText()));return result;
    }
    public synchronized ObjectNode draft(String id,JsonNode payload) throws Exception {
        ObjectNode entry;
        if(id==null) {entry=JSON.createObjectNode();entry.put("id",UUID.randomUUID().toString());entry.put("createdAt",Instant.now().toString());entry.put("kind","CREATE_DEMANDE");entry.put("state","DRAFT");}
        else {entry=(ObjectNode)get("entry:"+id);if(!"DRAFT".equals(entry.path("state").asText()))throw new IllegalArgumentException("Une opération envoyée ou en attente est immuable.");}
        entry.set("payload",payload);put("entry:"+entry.path("id").asText(),entry);return entry;
    }
    public synchronized void queue(String id) throws Exception {
        var entry=(ObjectNode)get("entry:"+id);
        if(!"DRAFT".equals(entry.path("state").asText()))throw new IllegalArgumentException("Seul un brouillon peut être envoyé.");
        entry.set("payload",RequestValidation.forSubmission(entry.path("payload")));
        entry.put("state","PENDING");put("entry:"+id,entry);
    }
    public synchronized ObjectNode queueCash(JsonNode demand) throws Exception {
        String target=demand.path("publicId").asText();if(target.isBlank())throw new IllegalArgumentException("Demande serveur obligatoire");
        for(var e:entries())if("CASH_PAYMENT".equals(e.path("kind").asText()) && !Set.of("REJECTED","SUPERSEDED","ARCHIVED").contains(e.path("state").asText()) && target.equals(e.path("payload").path("demandePublicId").asText()))
            throw new IllegalArgumentException("Un encaissement existe déjà dans la file pour cette demande.");
        if("PAYE".equals(demand.path("statutPaiement").asText()))throw new IllegalArgumentException("Cette demande est déjà payée.");
        if(demand.has("paiementDisponible")&&!demand.path("paiementDisponible").asBoolean())throw new IllegalArgumentException("Cette demande n'autorise pas encore l'encaissement.");
        String dependency=null;
        for(var e:entries())if("PENDING".equals(e.path("state").asText())&&target.equals(e.path("payload").path("demandePublicId").asText())){
            if("VALIDATE_DEMANDE".equals(e.path("kind").asText())&&"VALIDEE".equals(e.path("payload").path("statut").asText()))dependency=e.path("id").asText();
            else throw new IllegalArgumentException("Une opération est déjà en attente pour cette demande. Transmettez-la avant de saisir un encaissement.");
        }
        int amount;try{amount=demand.path("montant").decimalValue().intValueExact();}catch(Exception e){throw new IllegalArgumentException("Le montant de la demande est invalide. Actualisez la liste.");}
        if(amount<=0)throw new IllegalArgumentException("Actualisez la liste pour vérifier le montant à encaisser.");
        var payload=JSON.createObjectNode().put("demandePublicId",target).put("expectedMontant",amount);
        var entry=JSON.createObjectNode().put("id",UUID.randomUUID().toString()).put("kind","CASH_PAYMENT").put("state","PENDING").put("createdAt",Instant.now().toString());
        if(dependency!=null)entry.put("dependsOn",dependency);
        entry.set("payload",payload);entry.put("parish",demand.path("paroissePublicId").asText());entry.put("codeSuivie",demand.path("codeSuivie").asText());put("entry:"+entry.path("id").asText(),entry);return entry;
    }
    public synchronized void returnToDraft(String id)throws Exception{
        var entry=(ObjectNode)get("entry:"+id);
        if(!"CREATE_DEMANDE".equals(entry.path("kind").asText())||!"PENDING".equals(entry.path("state").asText())||entry.path("attempts").asInt()!=0)throw new IllegalArgumentException("Cette demande a déjà fait l'objet d'une tentative de transmission. Elle ne peut plus être remise en brouillon.");
        for(var child:entries())if(id.equals(child.path("localRequestId").asText())&&!id.equals(child.path("id").asText()))throw new IllegalArgumentException("Cette demande possède des opérations liées et ne peut plus revenir au brouillon.");
        if(entry.has("localSnapshot"))throw new IllegalArgumentException("Cette demande a été finalisée sur le poste et doit conserver sa référence.");
        entry.put("state","DRAFT");put("entry:"+id,entry);
    }
    public synchronized void retryBlocked(String id) throws Exception {
        var entry=(ObjectNode)get("entry:"+id);
        if(!"BLOCKED".equals(entry.path("state").asText()))throw new IllegalArgumentException("Seules les opérations bloquées peuvent être retentées avec le même identifiant.");
        entry.put("state","PENDING");entry.remove("error");put("entry:"+id,entry);
    }
    public synchronized void archive(String id) throws Exception {
        if(cashEvidence(id))throw new IllegalArgumentException("Un enregistrement d’espèces doit rester disponible pour le rapprochement et ne peut pas être archivé.");
        var e=(ObjectNode)get("entry:"+id);
        if(!"DRAFT".equals(e.path("state").asText()) && !"REJECTED".equals(e.path("state").asText()))throw new IllegalArgumentException("Impossible d'archiver une opération en cours ou confirmée.");
        e.put("state","ARCHIVED");put("entry:"+id,e);
    }
    public synchronized ObjectNode queueMutation(String kind,JsonNode demand,String intention,String decision) throws Exception {
        if(!Set.of("UPDATE_INTENTION","VALIDATE_DEMANDE","DELETE_DEMANDE").contains(kind))throw new IllegalArgumentException("Opération inconnue");
        String target=demand.path("publicId").asText(),version=demand.path("updatedAt").asText();
        if(target.isBlank() || version.isBlank())throw new IllegalArgumentException("Actualisez la demande avant de la modifier.");
        for(var e:entries())if("PENDING".equals(e.path("state").asText()) && target.equals(e.path("payload").path("demandePublicId").asText()))
            throw new IllegalArgumentException("Une opération est déjà en attente pour cette demande. Synchronisez d'abord.");
        var payload=JSON.createObjectNode().put("demandePublicId",target).put("expectedUpdatedAt",version);
        if(intention!=null)payload.put("intention",intention);if(decision!=null)payload.put("statut",decision);
        var entry=JSON.createObjectNode().put("id",UUID.randomUUID().toString()).put("kind",kind).put("state","PENDING").put("createdAt",Instant.now().toString()).put("codeSuivie",demand.path("codeSuivie").asText());
        entry.set("payload",payload);entry.put("parish",demand.path("paroissePublicId").asText());put("entry:"+entry.path("id").asText(),entry);return entry;
    }
    @Override public synchronized void close(){if(parishJournal!=null){parishJournal.close();parishJournal=null;}if(key!=null){Arrays.fill(key,(byte)0);key=null;}}
}
