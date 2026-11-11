package com.missanye.desktop.service;
import com.fasterxml.jackson.databind.*;
import com.missanye.desktop.api.ApiClient;
import com.missanye.desktop.config.ParishDeployment;
import com.missanye.desktop.repository.LocalStore;
import java.nio.file.Path;
import java.util.Arrays;

/** Normal parish login: server identity, parish membership, then protected local workspace. */
public final class ParishLoginService{
 public static final class ReauthenticationRequired extends IllegalArgumentException{public ReauthenticationRequired(String message){super(message);}}
 public static final class PreparationUnavailable extends Exception {
  public PreparationUnavailable(Exception cause){super("Vos identifiants ont été acceptés, mais le chargement de l’espace paroissial a échoué"+(cause instanceof ApiClient.ApiException response?" (HTTP "+response.status()+")":"")+". Réessayez ; si le problème persiste, contactez votre administrateur.",cause);}
 }
 public record Result(ApiClient api,LocalStore store,JsonNode session,JsonNode parishes,String parish){}
 public static final class LegacyMigrationRequired extends IllegalArgumentException{
  private final DeviceCredentials.Ticket ticket;private final String username;private final JsonNode session,parishes;
  public LegacyMigrationRequired(String username,DeviceCredentials.Ticket ticket,JsonNode session,JsonNode parishes){super("Vos anciens brouillons doivent être rattachés à cette nouvelle connexion.");this.username=username;this.ticket=ticket;this.session=session;this.parishes=parishes;}
  public DeviceCredentials.Ticket ticket(){return ticket;}public String username(){return username;}
 }
 public static void migrate(Path file,ParishDeployment deployment,DeviceCredentials credentials,LegacyMigrationRequired migration,char[] previousSecret)throws Exception{
  String key=new String(previousSecret);try(var store=LocalStore.unlock(file,deployment.apiUrl(),migration.username(),previousSecret,migration.ticket().owner())){
   store.saveSession(migration.session,migration.parishes);var t=migration.ticket();credentials.save(deployment.apiUrl(),migration.username(),new DeviceCredentials.Ticket(t.owner(),key,t.salt(),t.verifier(),t.expiresAt()));
  }finally{Arrays.fill(previousSecret,'\0');}
 }
 private static final ObjectMapper JSON=new ObjectMapper();
 private ParishLoginService(){}
 public static Result login(Path file,ParishDeployment deployment,DeviceCredentials credentials,String username,char[] password)throws Exception{
  var api=new ApiClient(deployment.apiUrl());LocalStore store=null;
  try{
   JsonNode session;
   boolean identityReady=false;
   try{credentials.verify(deployment.apiUrl(),username,password);identityReady=LocalStore.inspect(file,deployment.apiUrl(),username)==LocalStore.ProfileStatus.READY;}catch(Exception ignored){/* First use or changed password keeps the full online authentication path. */}
   try{session=api.login(username,new String(password),identityReady?java.time.Duration.ofSeconds(4):java.time.Duration.ofMinutes(2));}
   catch(Exception failure){
    api.clear();if(failure instanceof ApiClient.ApiException refused&&(refused.status()==401||refused.status()==403)){try{credentials.verify(deployment.apiUrl(),username,password);credentials.revoke(deployment.apiUrl(),username);}catch(Exception ignored){/* A wrong password must not invalidate another user's prepared identity. */}}
    if(!DeviceCredentials.unavailable(failure)||credentials.read(deployment.apiUrl(),username)==null)throw failure;
    var ticket=credentials.verify(deployment.apiUrl(),username,password);store=LocalStore.unlock(file,deployment.apiUrl(),username,ticket.key().toCharArray(),ticket.owner());
    if(!store.workspaceReady(deployment.parishId()))throw new IllegalArgumentException("Une première connexion au service est nécessaire sur ce poste.");
    return new Result(null,store,store.get("session"),onlyParish(store.get("parishes"),deployment.parishId()),deployment.parishId());
   }
   String owner=session.path("user").path("id").asText();if(owner.isBlank())throw new IllegalArgumentException("La session renvoyée par le service est incomplète.");
   var accessible=session.path("user").path("isGlobal").asBoolean()?api.get("paroisses"):session.path("paroisses");
   var allowed=onlyParish(accessible,deployment.parishId());if(allowed.isEmpty()){credentials.revoke(deployment.apiUrl(),username);throw new IllegalArgumentException("Ce compte n'est pas autorisé pour la paroisse configurée sur ce poste. Contactez l'administrateur de la paroisse.");}
   boolean read=false;for(var permission:session.path("user").path("permissions"))if("demand:read".equals(permission.asText()))read=true;
   if(!read){credentials.revoke(deployment.apiUrl(),username);throw new IllegalArgumentException("Ce compte ne dispose pas des accès de consultation nécessaires à cet espace.");}
   var ticket=credentials.prepare(deployment.apiUrl(),username,password,owner);
   if(credentials.read(deployment.apiUrl(),username)==null&&LocalStore.inspect(file,deployment.apiUrl(),username)!=LocalStore.ProfileStatus.ABSENT)throw new LegacyMigrationRequired(username,ticket,session,allowed);
   WorkspaceBootstrap.Prepared prepared;
   try{prepared=WorkspaceBootstrap.fetch(api,session,deployment.parishId());}
   catch(Exception failure){
    if(DeviceCredentials.unavailable(failure)&&credentials.read(deployment.apiUrl(),username)!=null&&LocalStore.inspect(file,deployment.apiUrl(),username)==LocalStore.ProfileStatus.READY){
     // Authentication, parish membership and current permissions were checked above.
     // Reuse the complete cached workspace; never replace it with partially loaded data.
     api.clear();store=LocalStore.unlock(file,deployment.apiUrl(),username,ticket.key().toCharArray(),owner);
     if(!store.workspaceReady(deployment.parishId()))throw failure;
     store.saveSession(session,allowed);credentials.save(deployment.apiUrl(),username,ticket);
     return new Result(null,store,session,allowed,deployment.parishId());
    }
    if(DeviceCredentials.unavailable(failure))throw new PreparationUnavailable(failure);
    throw failure;
   }
   // Persist the protected key before creating a vault so an interrupted installation cannot lose it.
   credentials.save(deployment.apiUrl(),username,ticket);store=LocalStore.unlock(file,deployment.apiUrl(),username,ticket.key().toCharArray(),owner);store.installWorkspace(prepared.session(),allowed,deployment.parishId(),prepared);
   return new Result(api,store,prepared.session(),allowed,deployment.parishId());
  }catch(Exception failure){api.clear();if(store!=null)store.close();throw failure;}finally{Arrays.fill(password,'\0');}
 }
 public record Reconnected(ApiClient api,JsonNode session,JsonNode parishes,DeviceCredentials.Ticket ticket){}
 public static Reconnected authenticateReconnect(String owner,ParishDeployment deployment,DeviceCredentials credentials,String username,char[] password)throws Exception{
  var api=new ApiClient(deployment.apiUrl());try{
   var session=api.login(username,new String(password));if(!owner.equals(session.path("user").path("id").asText()))throw new SyncService.SessionMismatchException();
   var accessible=session.path("user").path("isGlobal").asBoolean()?api.get("paroisses"):session.path("paroisses");var allowed=onlyParish(accessible,deployment.parishId());
   boolean read=false;for(var p:session.path("user").path("permissions"))if("demand:read".equals(p.asText()))read=true;
   if(allowed.isEmpty()||!read){credentials.revoke(deployment.apiUrl(),username);throw new IllegalArgumentException("Vos accès à cette paroisse ont changé. Reconnectez-vous après vérification avec votre administrateur.");}
   return new Reconnected(api,session,allowed,credentials.prepare(deployment.apiUrl(),username,password,owner));
  }catch(Exception failure){api.clear();if(failure instanceof IllegalArgumentException||(failure instanceof ApiClient.ApiException denied&&(denied.status()==401||denied.status()==403)))throw new ReauthenticationRequired(failure.getMessage());throw failure;}finally{Arrays.fill(password,'\0');}
 }
 private static com.fasterxml.jackson.databind.node.ArrayNode onlyParish(JsonNode parishes,String id){var result=JSON.createArrayNode();for(var p:WorkspaceBootstrap.eligible(parishes))if(id.equals(WorkspaceBootstrap.parishId(p)))result.add(p);return result;}
}
