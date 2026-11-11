package com.missanye.desktop;
import com.fasterxml.jackson.databind.*;
import com.missanye.desktop.api.ApiClient;
import com.missanye.desktop.config.ParishDeployment;
import com.missanye.desktop.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.sun.net.httpserver.*;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;
class ParishLoginServiceTest{
 @TempDir Path directory;
 private HttpServer server;private String url;private final AtomicInteger loginStatus=new AtomicInteger(200);private final AtomicBoolean allowed=new AtomicBoolean(true);private final AtomicInteger demandsStatus=new AtomicInteger(200);
 private static final String PARISH=TestRequests.PARISH;
 @BeforeEach void start()throws Exception{
  server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);var json=new ObjectMapper();var payload=TestRequests.complete();String type=payload.path("typeDemandePublicId").asText();
  server.createContext("/auth/login-multi-tenant",e->reply(e,loginStatus.get(),"{}"));server.createContext("/auth/me",e->reply(e,200,"{\"user\":{\"id\":\"owner\",\"username\":\"alice\",\"role\":\"SECRETAIRE\",\"permissions\":[\"demand:read\",\"demand:edit\"]},\"paroisses\":"+(allowed.get()?"[{\"id\":\""+PARISH+"\",\"nom\":\"Saint Paul\"}]":"[]")+"}"));
  server.createContext("/type-demandes/",e->reply(e,200,"[{\"publicId\":\""+type+"\"}]"));server.createContext("/horaires/",e->reply(e,200,"[]"));server.createContext("/type-paiement",e->reply(e,200,"[]"));server.createContext("/forfait-tarifs/",e->reply(e,200,"[]"));server.createContext("/demandes/",e->reply(e,demandsStatus.get(),"{\"content\":[],\"last\":true}"));server.start();url="http://127.0.0.1:"+server.getAddress().getPort()+"/";
 }
 @AfterEach void stop(){server.stop(0);}
 static void reply(HttpExchange e,int status,String body)throws java.io.IOException{byte[] bytes=body.getBytes(StandardCharsets.UTF_8);e.sendResponseHeaders(status,bytes.length);e.getResponseBody().write(bytes);e.close();}
 @Test void normalPasswordOpensOnlyBoundParishAndNetworkFailureUsesPreparedIdentity()throws Exception{
  var deployment=new ParishDeployment(url,PARISH,"Saint Paul");var credentials=new DeviceCredentials(directory.resolve("identity"),DeviceCredentialsTest.testProtector());Path file=directory.resolve("workspace.sqlite");
  var online=ParishLoginService.login(file,deployment,credentials,"alice","password".toCharArray());assertNotNull(online.api());assertEquals(1,online.parishes().size());assertTrue(online.store().workspaceReady(PARISH));online.store().close();online.api().clear();
  loginStatus.set(503);var local=ParishLoginService.login(file,deployment,credentials,"alice","password".toCharArray());assertNull(local.api());assertEquals(PARISH,local.parish());local.store().close();
  loginStatus.set(401);assertThrows(ApiClient.ApiException.class,()->ParishLoginService.login(file,deployment,credentials,"alice","password".toCharArray()));
 }
 @Test void membershipDenialDoesNotCreateVaultOrLocalIdentity()throws Exception{
  allowed.set(false);var credentials=new DeviceCredentials(directory.resolve("identity"),DeviceCredentialsTest.testProtector());Path file=directory.resolve("workspace.sqlite");var failure=assertThrows(IllegalArgumentException.class,()->ParishLoginService.login(file,new ParishDeployment(url,PARISH,"Saint Paul"),credentials,"alice","password".toCharArray()));assertTrue(failure.getMessage().contains("pas autorisé"));assertFalse(java.nio.file.Files.exists(file));assertNull(credentials.read(url,"alice"));
 }
 @Test void existingDraftsSurviveOneTimeMigration()throws Exception{
  Path file=directory.resolve("legacy.sqlite");String draft;
  try(var old=com.missanye.desktop.repository.LocalStore.unlock(file,url,"alice","Old local password!".toCharArray(),"owner")){draft=old.draft(null,TestRequests.complete()).path("id").asText();}
  var binding=new ParishDeployment(url,PARISH,"Saint Paul");var credentials=new DeviceCredentials(directory.resolve("identity"),DeviceCredentialsTest.testProtector());
  var required=assertThrows(ParishLoginService.LegacyMigrationRequired.class,()->ParishLoginService.login(file,binding,credentials,"alice","server password".toCharArray()));assertNull(credentials.read(url,"alice"));
  ParishLoginService.migrate(file,binding,credentials,required,"Old local password!".toCharArray());var opened=ParishLoginService.login(file,binding,credentials,"alice","server password".toCharArray());assertEquals("DRAFT",opened.store().get("entry:"+draft).path("state").asText());opened.store().close();opened.api().clear();
 }
 @Test void installationNeverTransmitsPendingWorkOfAnotherParish()throws Exception{
  Path file=directory.resolve("scoped.sqlite");var credentials=new DeviceCredentials(directory.resolve("identity"),DeviceCredentialsTest.testProtector());var opened=ParishLoginService.login(file,new ParishDeployment(url,PARISH,"Saint Paul"),credentials,"alice","password".toCharArray());
  try(var store=opened.store()){
   var payload=TestRequests.complete().put("paroissePublicId","731c7ab4-2fae-4d78-a17b-0ff7653f4599");String id=store.draft(null,payload).path("id").asText();store.queue(id);new SyncService(store,opened.api(),PARISH).synchronize();assertEquals("PENDING",store.get("entry:"+id).path("state").asText());assertEquals(0,store.get("entry:"+id).path("attempts").asInt());
  }finally{opened.api().clear();}
 }

 @Test void knownMembershipRevocationDisablesPreparedIdentity()throws Exception{
  Path file=directory.resolve("revocation.sqlite");var credentials=new DeviceCredentials(directory.resolve("identity"),DeviceCredentialsTest.testProtector());var deployment=new ParishDeployment(url,PARISH,"Saint Paul");var opened=ParishLoginService.login(file,deployment,credentials,"alice","password".toCharArray());opened.store().close();opened.api().clear();
  allowed.set(false);assertThrows(IllegalArgumentException.class,()->ParishLoginService.login(file,deployment,credentials,"alice","password".toCharArray()));assertEquals(0,credentials.read(url,"alice").expiresAt());loginStatus.set(503);assertThrows(IllegalArgumentException.class,()->ParishLoginService.login(file,deployment,credentials,"alice","password".toCharArray()));
 }

 @Test void preparedIdentityDoesNotWaitForSlowServerStartup()throws Exception{
  var deployment=new ParishDeployment(url,PARISH,"Saint Paul");var credentials=new DeviceCredentials(directory.resolve("identity"),DeviceCredentialsTest.testProtector());Path file=directory.resolve("slow.sqlite");
  var prepared=ParishLoginService.login(file,deployment,credentials,"alice","password".toCharArray());prepared.store().close();prepared.api().clear();
  server.removeContext("/auth/login-multi-tenant");var release=new java.util.concurrent.CountDownLatch(1);
  server.createContext("/auth/login-multi-tenant",e->{try{release.await(6,java.util.concurrent.TimeUnit.SECONDS);reply(e,503,"{}");}catch(Exception ignored){e.close();}});
  try{
   var opened=assertTimeoutPreemptively(java.time.Duration.ofSeconds(7),()->ParishLoginService.login(file,deployment,credentials,"alice","password".toCharArray()));
   assertNull(opened.api());assertTrue(opened.store().workspaceReady(PARISH));opened.store().close();
  }finally{release.countDown();}
 }

 @Test void internalServerFailureUsesOnlyPreviouslyVerifiedIdentity()throws Exception{
  var deployment=new ParishDeployment(url,PARISH,"Saint Paul");var credentials=new DeviceCredentials(directory.resolve("identity"),DeviceCredentialsTest.testProtector());Path file=directory.resolve("server500.sqlite");
  loginStatus.set(500);var first=assertThrows(ApiClient.ApiException.class,()->ParishLoginService.login(file,deployment,credentials,"alice","password".toCharArray()));assertEquals(500,first.status());assertFalse(java.nio.file.Files.exists(file));assertNull(credentials.read(url,"alice"));
  loginStatus.set(200);var prepared=ParishLoginService.login(file,deployment,credentials,"alice","password".toCharArray());String id=prepared.store().draft(null,TestRequests.complete()).path("id").asText();prepared.store().close();prepared.api().clear();
  loginStatus.set(500);var opened=ParishLoginService.login(file,deployment,credentials,"alice","password".toCharArray());assertNull(opened.api());assertEquals("DRAFT",opened.store().get("entry:"+id).path("state").asText());opened.store().close();
  assertThrows(IllegalArgumentException.class,()->ParishLoginService.login(file,deployment,credentials,"alice","wrong".toCharArray()));
  loginStatus.set(403);assertThrows(ApiClient.ApiException.class,()->ParishLoginService.login(file,deployment,credentials,"alice","password".toCharArray()));loginStatus.set(500);assertThrows(IllegalArgumentException.class,()->ParishLoginService.login(file,deployment,credentials,"alice","password".toCharArray()));
 }
 @Test void acceptedLoginCanReopenCompleteCacheWhenBootstrapFails()throws Exception{
  var deployment=new ParishDeployment(url,PARISH,"Saint Paul");var credentials=new DeviceCredentials(directory.resolve("identity"),DeviceCredentialsTest.testProtector());Path file=directory.resolve("bootstrap.sqlite");
  var prepared=ParishLoginService.login(file,deployment,credentials,"alice","password".toCharArray());String id=prepared.store().draft(null,TestRequests.complete()).path("id").asText();var catalogue=prepared.store().get("catalogue:"+PARISH);prepared.store().close();prepared.api().clear();
  demandsStatus.set(500);var opened=ParishLoginService.login(file,deployment,credentials,"alice","password".toCharArray());assertNull(opened.api());assertEquals(catalogue,opened.store().get("catalogue:"+PARISH));assertEquals("DRAFT",opened.store().get("entry:"+id).path("state").asText());assertTrue(opened.store().workspaceReady(PARISH));opened.store().close();
  demandsStatus.set(403);assertThrows(ApiClient.ApiException.class,()->ParishLoginService.login(file,deployment,credentials,"alice","password".toCharArray()));
 }
 @Test void firstUseReportsAcceptedCredentialsSeparatelyFromUnavailableWorkspace()throws Exception{
  demandsStatus.set(503);var credentials=new DeviceCredentials(directory.resolve("identity"),DeviceCredentialsTest.testProtector());Path file=directory.resolve("first.sqlite");
  var failure=assertThrows(ParishLoginService.PreparationUnavailable.class,()->ParishLoginService.login(file,new ParishDeployment(url,PARISH,"Saint Paul"),credentials,"alice","password".toCharArray()));
  assertTrue(OperationErrors.message(failure).contains("acceptés"));assertTrue(OperationErrors.message(failure).contains("HTTP 503"));assertFalse(java.nio.file.Files.exists(file));assertNull(credentials.read(url,"alice"));
 }

}
