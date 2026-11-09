package com.missanye.desktop.repository;

import com.fasterxml.jackson.databind.*;
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
        scope=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((server+"\n"+username).getBytes(StandardCharsets.UTF_8)));
        try(var c=connect(); var s=c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS vault_profile(scope TEXT PRIMARY KEY,salt BLOB NOT NULL,proof BLOB NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS vault_data(scope TEXT NOT NULL,name TEXT NOT NULL,value BLOB NOT NULL,PRIMARY KEY(scope,name))");
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
    public void put(String name,JsonNode value) throws Exception {
        byte[] encrypted=encrypt(name,JSON.writeValueAsBytes(value));
        try(var c=connect();var s=c.prepareStatement("INSERT INTO vault_data VALUES(?,?,?) ON CONFLICT(scope,name) DO UPDATE SET value=excluded.value")) {
            s.setString(1,scope);s.setString(2,name);s.setBytes(3,encrypted);s.executeUpdate();
        }
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
        entry.put("state","PENDING");put("entry:"+id,entry);
    }
    public ObjectNode queueCash(JsonNode demand) throws Exception {
        String target=demand.path("publicId").asText();if(target.isBlank())throw new IllegalArgumentException("Demande serveur obligatoire");
        for(var e:entries())if("CASH_PAYMENT".equals(e.path("kind").asText()) && !"REJECTED".equals(e.path("state").asText()) && target.equals(e.path("payload").path("demandePublicId").asText()))
            throw new IllegalArgumentException("Un encaissement existe déjà dans la file pour cette demande.");
        var payload=JSON.createObjectNode().put("demandePublicId",target).put("expectedMontant",demand.path("montant").intValue());
        var entry=JSON.createObjectNode().put("id",UUID.randomUUID().toString()).put("kind","CASH_PAYMENT").put("state","PENDING").put("createdAt",Instant.now().toString());
        entry.set("payload",payload);entry.put("codeSuivie",demand.path("codeSuivie").asText());put("entry:"+entry.path("id").asText(),entry);return entry;
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
