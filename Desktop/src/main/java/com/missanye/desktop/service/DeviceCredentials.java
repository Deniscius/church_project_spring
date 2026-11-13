package com.missanye.desktop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

/** Per Windows user, server and account. Stores no server password or session token. */
public final class DeviceCredentials {
    public interface Protector{byte[] protect(byte[] value)throws Exception;byte[] unprotect(byte[] value)throws Exception;}
    public record Ticket(String owner,String key,String salt,String verifier,long expiresAt){}
    private static final ObjectMapper JSON=new ObjectMapper();
    private final Path directory;private final Protector protector;
    public DeviceCredentials(Path directory,Protector protector){this.directory=directory;this.protector=protector;}
    public static DeviceCredentials windows(Path database){return new DeviceCredentials(database.toAbsolutePath().getParent().resolve("identities"),new WindowsProtector());}
    private Path path(String server,String username)throws Exception{return directory.resolve(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((server+"\n"+username.strip().toLowerCase(Locale.ROOT)).getBytes(StandardCharsets.UTF_8)))+".dpapi");}
    /** Shared only within this Windows profile and this server/parish; account verifiers remain private. */
    public synchronized char[] parishKey(String server,String parish)throws Exception{
        java.util.UUID.fromString(parish);Files.createDirectories(directory);
        String id=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((server+"\nparish:"+parish).getBytes(StandardCharsets.UTF_8)));
        Path target=directory.resolve("parish-"+id+".dpapi");
        if(Files.exists(target))return new String(protector.unprotect(Files.readAllBytes(target)),StandardCharsets.US_ASCII).toCharArray();
        byte[] random=new byte[32];new SecureRandom().nextBytes(random);String key=Base64.getEncoder().encodeToString(random);byte[] sealed=protector.protect(key.getBytes(StandardCharsets.US_ASCII));
        Path temp=Files.createTempFile(directory,"parish-",".tmp");try{Files.write(temp,sealed);Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE);}finally{Files.deleteIfExists(temp);}
        return key.toCharArray();
    }
    public Ticket read(String server,String username)throws Exception{Path file=path(server,username);return Files.exists(file)?JSON.readValue(protector.unprotect(Files.readAllBytes(file)),Ticket.class):null;}
    public Ticket prepare(String server,String username,char[] password,String owner)throws Exception{
        Ticket previous=read(server,username);if(previous!=null&&!owner.equals(previous.owner()))throw new IllegalArgumentException("L'identité serveur de ce compte a changé. Contactez votre administrateur.");
        byte[] salt=new byte[32],key=new byte[32];new SecureRandom().nextBytes(salt);new SecureRandom().nextBytes(key);
        return new Ticket(owner,previous==null?Base64.getEncoder().encodeToString(key):previous.key(),Base64.getEncoder().encodeToString(salt),Base64.getEncoder().encodeToString(derive(password,salt)),Instant.now().plus(Duration.ofHours(72)).getEpochSecond());
    }
    public void save(String server,String username,Ticket ticket)throws Exception{
        Files.createDirectories(directory);byte[] sealed=protector.protect(JSON.writeValueAsBytes(ticket));Path target=path(server,username),temporary=Files.createTempFile(directory,"identity-",".tmp");
        try{Files.write(temporary,sealed);try{Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(temporary);}
    }
    public void revoke(String server,String username)throws Exception{var ticket=read(server,username);if(ticket!=null)save(server,username,new Ticket(ticket.owner(),ticket.key(),ticket.salt(),ticket.verifier(),0));}
    public Ticket verify(String server,String username,char[] password)throws Exception{
        var ticket=read(server,username);if(ticket==null||ticket.expiresAt()<=Instant.now().getEpochSecond())throw new IllegalArgumentException("Une connexion au service est nécessaire pour vérifier ce compte. Réessayez lorsque le service est disponible.");
        if(!MessageDigest.isEqual(Base64.getDecoder().decode(ticket.verifier()),derive(password,Base64.getDecoder().decode(ticket.salt()))))throw new IllegalArgumentException("Identifiant ou mot de passe incorrect.");return ticket;
    }
    private static byte[] derive(char[] password,byte[] salt)throws Exception{var spec=new PBEKeySpec(password,salt,210000,256);try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}finally{spec.clearPassword();}}
    public static boolean unavailable(Throwable failure){return failure instanceof java.net.ConnectException||failure instanceof java.net.UnknownHostException||failure instanceof java.net.http.HttpTimeoutException||(failure instanceof com.missanye.desktop.api.ApiClient.ApiException e&&Set.of(500,502,503,504).contains(e.status()));}
    public static final class WindowsProtector implements Protector{
        public byte[] protect(byte[] value)throws Exception{return run(value,true);}public byte[] unprotect(byte[] value)throws Exception{return run(value,false);}
        private byte[] run(byte[] value,boolean seal)throws Exception{
            if(!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows"))throw new IllegalStateException("La protection du poste nécessite Windows.");
            String script="$ErrorActionPreference='Stop'; Add-Type -AssemblyName System.Security; $b=[Convert]::FromBase64String([Console]::ReadLine()); $r=[Security.Cryptography.ProtectedData]::"+(seal?"Protect":"Unprotect")+"($b,$null,[Security.Cryptography.DataProtectionScope]::CurrentUser); [Console]::Write([Convert]::ToBase64String($r))";
            var process=new ProcessBuilder("powershell.exe","-NoProfile","-NonInteractive","-Command",script).start();
            try{try(var input=process.getOutputStream()){input.write((Base64.getEncoder().encodeToString(value)+"\n").getBytes(StandardCharsets.US_ASCII));}
                if(!process.waitFor(20,java.util.concurrent.TimeUnit.SECONDS))throw new java.io.IOException("La protection du poste n'a pas répondu.");
                if(process.exitValue()!=0)throw new java.io.IOException("La protection Windows du poste a échoué.");return Base64.getDecoder().decode(new String(process.getInputStream().readAllBytes(),StandardCharsets.US_ASCII).strip());
            }finally{process.destroy();}
        }
    }
}
