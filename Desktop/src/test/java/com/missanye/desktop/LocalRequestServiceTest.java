package com.missanye.desktop;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.missanye.desktop.repository.LocalStore;
import com.missanye.desktop.service.*;
import com.missanye.desktop.api.ApiClient;
import com.sun.net.httpserver.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class LocalRequestServiceTest {
 @TempDir Path temp;static final ObjectMapper JSON=new ObjectMapper();static final String PASSWORD="Local test password!",REMOTE="a31c7ab4-2fae-4d78-a17b-0ff7653f4501";
 static ObjectNode session(String... permissions){var node=JSON.createObjectNode();var user=node.putObject("user").put("id","owner").put("username","alice");var list=user.putArray("permissions");for(var p:permissions)list.add(p);node.putArray("paroisses").addObject().put("id",TestRequests.PARISH).put("nom","Saint Paul");return node;}
 static final String[] RIGHTS={"demand:read","demand:edit","demand:validate","payment:manage","invoice:read"};
 static LocalStore open(Path path)throws Exception{return LocalStore.unlock(path,"http://localhost:8081/","alice",PASSWORD.toCharArray(),"owner");}
 static void seed(LocalStore store,boolean special)throws Exception{
  var s=session(RIGHTS);store.saveSession(s,s.path("paroisses"));var p=TestRequests.complete();var c=JSON.createObjectNode();c.putArray("types").addObject().put("publicId",p.path("typeDemandePublicId").asText()).put("libelle","Messe");
  c.putObject("forfaits").putArray(p.path("typeDemandePublicId").asText()).addObject().put("publicId",p.path("forfaitTarifPublicId").asText()).put("natureForfait",special?"SPECIALE":"NORMALE").put("nombreCelebration",1).put("montantForfait",2000);
  c.putArray("horaires").addObject().put("publicId",p.path("horairePublicId").asText()).put("heureCelebration","08:00:00");c.putArray("paiements").addObject().put("publicId",p.path("typePaiementPublicId").asText()).put("libelle","Espèces");store.put("catalogue:"+TestRequests.PARISH,c);store.put("demands:"+TestRequests.PARISH,JSON.createArrayNode());
 }
 static String finalizeRequest(LocalStore store,boolean special)throws Exception{seed(store,special);var p=TestRequests.complete().put("emailFidele","alice@example.test").put("nomFidele","AMEGA").put("prenomFidele","Alice");String id=store.draft(null,p).path("id").asText();new LocalRequestService(store).finalizeDraft(id);return id;}
 @Test void validateReceiveCashAndPrintWithoutNetworkSurviveRestart()throws Exception{
  Path file=temp.resolve("business.sqlite");String id;byte[] pdf;
  try(var store=open(file)){
   id=finalizeRequest(store,true);var business=new LocalRequestService(store);assertThrows(IllegalArgumentException.class,()->business.receiveCash(id));
   business.decide(id,"VALIDEE");business.receiveCash(id);assertThrows(IllegalArgumentException.class,()->business.receiveCash(id));assertThrows(IllegalArgumentException.class,()->store.returnToDraft(id));
   var view=business.view(id);assertTrue(view.path("localPending").asBoolean());assertEquals("ESPECES_ENREGISTREES",view.path("statutPaiement").asText());assertEquals(2000,view.path("localQuotedMontant").asInt());pdf=business.receipt(id);assertArrayEquals(pdf,business.receipt(id));
   Files.write(Path.of("target","desktop-local-receipt-preview.pdf"),pdf);var reader=new com.lowagie.text.pdf.PdfReader(pdf);String text=new com.lowagie.text.pdf.parser.PdfTextExtractor(reader).getTextFromPage(1);assertTrue(text.replaceAll("\\s+", "").contains(LocalRequestService.reference(id)),text);assertEquals(1,reader.getNumberOfPages());assertTrue(text.contains("ORIGINAL"));assertTrue(text.contains("DUPLICATA"));assertTrue(text.contains("REÇU DE DÉPÔT"));assertTrue(text.contains("Payée"));assertFalse(text.contains("TRANSMISSION"));assertFalse(text.contains("DOCUMENT LOCAL"));assertTrue(text.contains("Alice AMEGA"));assertTrue(text.contains("Messe"));reader.close();
  }
  try(var reopened=open(file)){assertEquals(3,reopened.entries().size());assertArrayEquals(pdf,new LocalRequestService(reopened).receipt(id));assertEquals("PENDING",reopened.get("entry:"+id).path("state").asText());}
 }
 @Test void roleChecksAndMissingPriceFailBeforeProducingBusinessFacts()throws Exception{
  try(var store=open(temp.resolve("rights.sqlite"))){seed(store,true);var s=session("demand:edit");store.saveSession(s,s.path("paroisses"));String id=store.draft(null,TestRequests.complete().put("emailFidele","a@example.test")).path("id").asText();var service=new LocalRequestService(store);service.finalizeDraft(id);assertThrows(IllegalArgumentException.class,()->service.decide(id,"VALIDEE"));assertThrows(IllegalArgumentException.class,()->service.receiveCash(id));assertThrows(IllegalArgumentException.class,()->service.receipt(id));assertEquals(1,store.entries().size());}
  try(var store=open(temp.resolve("price.sqlite"))){seed(store,false);var c=(ObjectNode)store.get("catalogue:"+TestRequests.PARISH);((ObjectNode)c.path("forfaits").elements().next().get(0)).put("montantForfait",-1);store.put("catalogue:"+TestRequests.PARISH,c);String id=store.draft(null,TestRequests.complete()).path("id").asText();assertThrows(IllegalArgumentException.class,()->new LocalRequestService(store).finalizeDraft(id));assertEquals("DRAFT",store.get("entry:"+id).path("state").asText());}
 }
 private static void reply(HttpExchange e,int status,JsonNode body)throws java.io.IOException{byte[] bytes=JSON.writeValueAsBytes(body);e.sendResponseHeaders(status,bytes.length);e.getResponseBody().write(bytes);e.close();}
 private ObjectNode response(String code,String validation,String payment){return TestRequests.complete().put("publicId",REMOTE).put("codeSuivie",code).put("montant",2000).put("statutValidation",validation).put("statutPaiement",payment).put("updatedAt","2026-11-10T10:00:00");}
 @Test void interruptedValidationReplaysSameCommandBeforeCashAndKeepsReference()throws Exception{
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);var calls=new ArrayList<JsonNode>();var failure=new AtomicBoolean(true);var info=session(RIGHTS);
  server.createContext("/auth/me",e->reply(e,200,info));server.createContext("/desktop/operations",e->{try{
   var command=JSON.readTree(e.getRequestBody());calls.add(command);String kind=command.path("kind").asText();
   if("VALIDATE_DEMANDE".equals(kind)&&failure.getAndSet(false)){reply(e,503,JSON.createObjectNode());return;}
   var response=response("MS-SP-ABCDEFGHJK","CREATE_DEMANDE".equals(kind)?"EN_ATTENTE":"VALIDEE","CASH_PAYMENT".equals(kind)?"PAYE":"NON_PAYE");if("CREATE_DEMANDE".equals(kind))response.put("localReference",LocalRequestService.reference(command.path("operationId").asText()));reply(e,200,response);
  }catch(Exception error){e.close();}});server.start();
  try(var store=open(temp.resolve("sync.sqlite"))){String id=finalizeRequest(store,true);var service=new LocalRequestService(store);service.decide(id,"VALIDEE");service.receiveCash(id);byte[] original=service.receipt(id);
   var sync=new SyncService(store,new ApiClient("http://127.0.0.1:"+server.getAddress().getPort()),TestRequests.PARISH);
   assertThrows(ApiClient.ApiException.class,sync::synchronize);assertEquals(2,calls.size());assertEquals("SENT",store.get("entry:"+id).path("state").asText());sync.synchronize();assertEquals(4,calls.size());assertEquals(calls.get(1),calls.get(2));assertEquals("CASH_PAYMENT",calls.get(3).path("kind").asText());assertEquals(REMOTE,calls.get(3).path("payload").path("demandePublicId").asText());
   sync.synchronize();assertEquals(4,calls.size());assertFalse(service.view(id).path("localPending").asBoolean());assertEquals(LocalRequestService.reference(id),service.view(id).path("localReference").asText());assertEquals("PAYE",service.view(id).path("statutPaiement").asText());assertTrue(original.length>100);
  }finally{server.stop(0);}
 }
 @Test void rejectedCreationStopsAllDependentsAndPreservesCashReceipt()throws Exception{
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);var calls=new AtomicInteger();server.createContext("/auth/me",e->reply(e,200,session(RIGHTS)));server.createContext("/desktop/operations",e->{calls.incrementAndGet();reply(e,409,JSON.createObjectNode().put("message","Créneau indisponible"));});server.start();
  try(var store=open(temp.resolve("reject.sqlite"))){String id=finalizeRequest(store,true);var local=new LocalRequestService(store);local.decide(id,"VALIDEE");local.receiveCash(id);var sync=new SyncService(store,new ApiClient("http://127.0.0.1:"+server.getAddress().getPort()),TestRequests.PARISH);sync.synchronize();sync.synchronize();assertEquals(1,calls.get());assertTrue(local.view(id).path("localReconciliationRequired").asBoolean());assertEquals(3,store.entries().size());assertTrue(local.receipt(id).length>100);assertThrows(IllegalArgumentException.class,()->local.receiveCash(id));assertThrows(IllegalArgumentException.class,()->store.archive(id));assertThrows(IllegalArgumentException.class,()->store.correctDraft(id,TestRequests.complete()));for(var entry:store.entries())if("CASH_PAYMENT".equals(entry.path("kind").asText()))assertThrows(IllegalArgumentException.class,()->store.archive(entry.path("id").asText()));}
  finally{server.stop(0);}
 }
 @Test void oldServerCannotAcknowledgeNewLocalReferenceUntilAliasIsSupported()throws Exception{
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);var updated=new AtomicBoolean();var calls=new ArrayList<JsonNode>();server.createContext("/auth/me",e->reply(e,200,session(RIGHTS)));server.createContext("/desktop/operations",e->{try{var command=JSON.readTree(e.getRequestBody());calls.add(command);var result=response("MS-SP-ABCDEFGHJK","VALIDEE","CASH_PAYMENT".equals(command.path("kind").asText())?"PAYE":"NON_PAYE");if(updated.get())result.put("localReference",LocalRequestService.reference(command.path("operationId").asText()));reply(e,200,result);}catch(Exception failure){e.close();}});server.start();
  try(var store=open(temp.resolve("upgrade.sqlite"))){String id=finalizeRequest(store,false);var local=new LocalRequestService(store);local.receiveCash(id);var sync=new SyncService(store,new ApiClient("http://127.0.0.1:"+server.getAddress().getPort()),TestRequests.PARISH);assertThrows(java.io.IOException.class,sync::synchronize);assertEquals(1,calls.size());assertEquals("PENDING",store.get("entry:"+id).path("state").asText());updated.set(true);sync.synchronize();assertEquals(3,calls.size());assertEquals(calls.get(0),calls.get(1));assertFalse(local.view(id).path("localPending").asBoolean());}
  finally{server.stop(0);}
 }
 @Test void pendingDecisionOnExistingDemandAllowsDependentCashReceipt()throws Exception{
  try(var store=open(temp.resolve("existing.sqlite"))){seed(store,true);var existing=response("MS-SP-ABCDEFGHJK","EN_ATTENTE","NON_PAYE").put("createdAt","2026-01-01T08:00:00").put("paiementDisponible",false).put("validationRequise",true);store.put("demands:"+TestRequests.PARISH,JSON.createArrayNode().add(existing));var decision=store.queueMutation("VALIDATE_DEMANDE",existing,null,"VALIDEE");var service=new LocalRequestService(store);var projected=service.overlay(TestRequests.PARISH,store.get("demands:"+TestRequests.PARISH)).get(0);assertTrue(projected.path("paiementDisponible").asBoolean());var cash=store.queueCash(projected);assertEquals(decision.path("id").asText(),cash.path("dependsOn").asText());var received=service.overlay(TestRequests.PARISH,store.get("demands:"+TestRequests.PARISH)).get(0);assertTrue(received.path("localCashReceived").asBoolean());assertEquals(cash.path("createdAt").asText(),received.path("localCashRecordedAt").asText());try(var pdf=new com.lowagie.text.pdf.PdfReader(service.receiptFor(received))){assertTrue(new com.lowagie.text.pdf.parser.PdfTextExtractor(pdf).getTextFromPage(1).contains("01/01/2026"));}assertEquals("NON_PAYE",store.get("demands:"+TestRequests.PARISH).get(0).path("statutPaiement").asText());}
 }

}
