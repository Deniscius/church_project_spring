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

/** Encrypted account/server-scoped cache and durable outbox. No session credential is stored. */
public final class LocalStore implements AutoCloseable {
    private static final ObjectMapper JSON=new ObjectMapper();
    private final String url, scope;
    private byte[] key;
    private String owner;
    private LocalStore(Path path, String server, String username) throws Exception {
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
    public String owner(){return owner;}
    public void saveSession(JsonNode session,JsonNode parishes)throws Exception{
        if(!owner.equals(session.path("user").path("id").asText()))throw new IllegalArgumentException("Ce compte ne correspond pas à cet espace de travail.");
        writeAll(Map.of("session",session,"parishes",parishes));
    }
    private boolean completeCatalogue(JsonNode c){return c.isObject()&&c.path("types").isArray()&&c.path("horaires").isArray()&&c.path("paiements").isArray()&&c.path("forfaits").isObject();}
    public boolean workspaceReady(String parish)throws Exception{
        var session=get("session");return owner.equals(session.path("user").path("id").asText())&&WorkspaceBootstrap.allows(get("parishes"),parish)&&completeCatalogue(get("catalogue:"+parish))&&get("demands:"+parish).isArray();
    }
    public void installWorkspace(JsonNode session,JsonNode parishes,String parish,WorkspaceBootstrap.Prepared prepared)throws Exception{
        if(!owner.equals(session.path("user").path("id").asText())||!owner.equals(prepared.session().path("user").path("id").asText())||!WorkspaceBootstrap.allows(parishes,parish))throw new IllegalArgumentException("Cet espace n'est pas autorisé pour le compte connecté.");
        if(!completeCatalogue(prepared.catalogue())||!prepared.demands().isArray())throw new IllegalArgumentException("La préparation de la paroisse est incomplète. Réessayez en ligne.");
        var marker=JSON.createObjectNode().put("version",1).put("owner",owner).put("parish",parish).put("preparedAt",Instant.now().toString());
        writeAll(Map.of("session",prepared.session(),"parishes",parishes,"catalogue:"+parish,prepared.catalogue(),"demands:"+parish,prepared.demands(),"ready:"+parish,marker,"selected-parish",JSON.getNodeFactory().textNode(parish)));
    }
    public void selectWorkspace(String parish)throws Exception{
        if(!workspaceReady(parish))throw new IllegalArgumentException("Une connexion est nécessaire pour préparer cette paroisse sur ce poste.");
        put("selected-parish",JSON.getNodeFactory().textNode(parish));
    }
    public ObjectNode correctDraft(String rejectedId,JsonNode payload)throws Exception{
        var original=(ObjectNode)get("entry:"+rejectedId);
        if(!"REJECTED".equals(original.path("state").asText())||!"CREATE_DEMANDE".equals(original.path("kind").asText()))throw new IllegalArgumentException("Cette demande a déjà été corrigée ou ne peut pas être modifiée.");
        var corrected=JSON.createObjectNode().put("id",UUID.randomUUID().toString()).put("createdAt",Instant.now().toString()).put("kind","CREATE_DEMANDE").put("state","DRAFT").put("correctedFrom",rejectedId);corrected.set("payload",payload);
        original.put("state","SUPERSEDED").put("replacedBy",corrected.path("id").asText());
        writeAll(Map.of("entry:"+rejectedId,original,"entry:"+corrected.path("id").asText(),corrected));return corrected;
    }

    public void put(String name,JsonNode value) throws Exception {writeAll(Map.of(name,value));}
    private void writeAll(Map<String,? extends JsonNode> values) throws Exception {
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
    public void confirm(ObjectNode entry,JsonNode result) throws Exception {
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
    public JsonNode get(String name) throws Exception {
        try(var c=connect();var s=c.prepareStatement("SELECT value FROM vault_data WHERE scope=? AND name=?")) {
            s.setString(1,scope);s.setString(2,name);try(var r=s.executeQuery()){return r.next()?JSON.readTree(decrypt(name,r.getBytes(1))):JSON.nullNode();}
        }
    }
    public List<ObjectNode> entries() throws Exception {
        var result=new ArrayList<ObjectNode>();
        try(var c=connect();var s=c.prepareStatement("SELECT name,value FROM vault_data WHERE scope=? AND name LIKE 'entry:%'")) {
            s.setString(1,scope);try(var r=s.executeQuery()){while(r.next())result.add((ObjectNode)JSON.readTree(decrypt(r.getString(1),r.getBytes(2))));}
        }
        result.sort(Comparator.comparing(e -> e.path("createdAt").asText()));return result;
    }
    public ObjectNode draft(String id,JsonNode payload) throws Exception {
        ObjectNode entry;
        if(id==null) {entry=JSON.createObjectNode();entry.put("id",UUID.randomUUID().toString());entry.put("createdAt",Instant.now().toString());entry.put("kind","CREATE_DEMANDE");entry.put("state","DRAFT");}
        else {entry=(ObjectNode)get("entry:"+id);if(!"DRAFT".equals(entry.path("state").asText()))throw new IllegalArgumentException("Une opération envoyée ou en attente est immuable.");}
        entry.set("payload",payload);put("entry:"+entry.path("id").asText(),entry);return entry;
    }
    public void queue(String id) throws Exception {
        var entry=(ObjectNode)get("entry:"+id);
        if(!"DRAFT".equals(entry.path("state").asText()))throw new IllegalArgumentException("Seul un brouillon peut être envoyé.");
        entry.set("payload",RequestValidation.forSubmission(entry.path("payload")));
        entry.put("state","PENDING");put("entry:"+id,entry);
    }
    public ObjectNode queueCash(JsonNode demand) throws Exception {
        String target=demand.path("publicId").asText();if(target.isBlank())throw new IllegalArgumentException("Demande serveur obligatoire");
        for(var e:entries())if("CASH_PAYMENT".equals(e.path("kind").asText()) && !Set.of("REJECTED","SUPERSEDED","ARCHIVED").contains(e.path("state").asText()) && target.equals(e.path("payload").path("demandePublicId").asText()))
            throw new IllegalArgumentException("Un encaissement existe déjà dans la file pour cette demande.");
        if("PAYE".equals(demand.path("statutPaiement").asText()))throw new IllegalArgumentException("Cette demande est déjà payée.");
        if(demand.has("paiementDisponible")&&!demand.path("paiementDisponible").asBoolean())throw new IllegalArgumentException("Cette demande n'autorise pas encore l'encaissement.");
        for(var e:entries())if("PENDING".equals(e.path("state").asText())&&target.equals(e.path("payload").path("demandePublicId").asText()))throw new IllegalArgumentException("Une opération est déjà en attente pour cette demande. Transmettez-la avant de saisir un encaissement.");
        int amount;try{amount=demand.path("montant").decimalValue().intValueExact();}catch(Exception e){throw new IllegalArgumentException("Le montant de la demande est invalide. Actualisez la liste.");}
        if(amount<=0)throw new IllegalArgumentException("Actualisez la liste pour vérifier le montant à encaisser.");
        var payload=JSON.createObjectNode().put("demandePublicId",target).put("expectedMontant",amount);
        var entry=JSON.createObjectNode().put("id",UUID.randomUUID().toString()).put("kind","CASH_PAYMENT").put("state","PENDING").put("createdAt",Instant.now().toString());
        entry.set("payload",payload);entry.put("parish",demand.path("paroissePublicId").asText());entry.put("codeSuivie",demand.path("codeSuivie").asText());put("entry:"+entry.path("id").asText(),entry);return entry;
    }
    public void returnToDraft(String id)throws Exception{
        var entry=(ObjectNode)get("entry:"+id);
        if(!"CREATE_DEMANDE".equals(entry.path("kind").asText())||!"PENDING".equals(entry.path("state").asText())||entry.path("attempts").asInt()!=0)throw new IllegalArgumentException("Cette demande a déjà fait l'objet d'une tentative de transmission. Elle ne peut plus être remise en brouillon.");
        entry.put("state","DRAFT");put("entry:"+id,entry);
    }
    public void retryBlocked(String id) throws Exception {
        var entry=(ObjectNode)get("entry:"+id);
        if(!"BLOCKED".equals(entry.path("state").asText()))throw new IllegalArgumentException("Seules les opérations bloquées peuvent être retentées avec le même identifiant.");
        entry.put("state","PENDING");entry.remove("error");put("entry:"+id,entry);
    }
    public void archive(String id) throws Exception {
        var e=(ObjectNode)get("entry:"+id);
        if(!"DRAFT".equals(e.path("state").asText()) && !"REJECTED".equals(e.path("state").asText()))throw new IllegalArgumentException("Impossible d'archiver une opération en cours ou confirmée.");
        e.put("state","ARCHIVED");put("entry:"+id,e);
    }
    public ObjectNode queueMutation(String kind,JsonNode demand,String intention,String decision) throws Exception {
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
    @Override public void close(){if(key!=null){Arrays.fill(key,(byte)0);key=null;}}
}
