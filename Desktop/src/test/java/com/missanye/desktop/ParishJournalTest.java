package com.missanye.desktop;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.missanye.desktop.repository.LocalStore;
import com.missanye.desktop.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ParishJournalTest {
    @TempDir Path directory;
    private final ObjectMapper json=new ObjectMapper();
    private final String server="https://parish.example/",parish=TestRequests.PARISH;
    private LocalStore account(String user,String parish,String... permissions)throws Exception{
        var store=LocalStore.unlock(directory.resolve("journal.sqlite"),server,user,"Local test password 123!".toCharArray(),user+"-id");
        var session=LocalRequestServiceTest.session(permissions);((ObjectNode)session.path("user")).put("id",user+"-id").put("username",user);
        ((ObjectNode)session.path("paroisses").get(0)).put("id",parish);
        var p=TestRequests.complete();var catalogue=json.createObjectNode();catalogue.putArray("types").addObject().put("publicId",p.path("typeDemandePublicId").asText()).put("libelle","Messe");catalogue.putArray("horaires");catalogue.putArray("paiements");
        catalogue.putObject("forfaits").putArray(p.path("typeDemandePublicId").asText()).addObject().put("publicId",p.path("forfaitTarifPublicId").asText()).put("natureForfait","SPECIALE").put("nombreCelebration",1).put("montantForfait",2000);
        store.installWorkspace(session,session.path("paroisses"),parish,new WorkspaceBootstrap.Prepared(session,catalogue,json.createArrayNode()));return store;
    }
    private void attach(LocalStore store,String parish)throws Exception{
        var credentials=new DeviceCredentials(directory.resolve("identities"),DeviceCredentialsTest.testProtector());store.attachParishJournal(parish,credentials.parishKey(server,parish),false);
    }
    @Test void tenantWorkflowIsSharedAcrossRolesAndKeepsIndividualAuthors()throws Exception{
        String id;
        try(var secretary=account("secretary",parish,"demand:read","demand:edit")){
            attach(secretary,parish);var view=new LocalRequestService(secretary).finalizePayload(null,TestRequests.complete().put("emailFidele","a@example.test"));id=view.path("localRequestId").asText();
            assertEquals("secretary-id",secretary.get("entry:"+id).path("createdBy").asText());
        }
        try(var validator=account("validator",parish,"demand:read","demand:validate")){
            attach(validator,parish);var service=new LocalRequestService(validator);service.decide(id,"VALIDEE");assertThrows(IllegalArgumentException.class,()->service.receiveCash(id));assertEquals(2,validator.entries().size());
        }
        try(var cashier=account("cashier",parish,"demand:read","payment:manage","invoice:read")){
            attach(cashier,parish);var service=new LocalRequestService(cashier);service.receiveCash(id);assertTrue(service.receipt(id).length>100);assertThrows(IllegalArgumentException.class,()->service.receiveCash(id));assertEquals(3,cashier.entries().size());var forged=(ObjectNode)cashier.get("entry:"+id).deepCopy();forged.put("kind","CASH_PAYMENT");assertThrows(IllegalArgumentException.class,()->cashier.put("entry:"+id,forged));
            assertEquals("secretary-id",cashier.get("entry:"+id).path("createdBy").asText());
            for(var e:cashier.entries())if("VALIDATE_DEMANDE".equals(e.path("kind").asText()))assertEquals("validator-id",e.path("createdBy").asText());else if("CASH_PAYMENT".equals(e.path("kind").asText()))assertEquals("cashier-id",e.path("createdBy").asText());
        }
        try(var reader=account("reader",parish,"demand:read")){
            attach(reader,parish);assertTrue(new LocalRequestService(reader).view(id).path("localCashReceived").asBoolean());assertEquals(3,reader.entries().size());
            assertThrows(IllegalArgumentException.class,()->reader.draft(null,TestRequests.complete()));assertThrows(IllegalArgumentException.class,()->reader.archive(id));assertThrows(IllegalArgumentException.class,()->new LocalRequestService(reader).receipt(id));
        }
        String other="731c7ab4-2fae-4d78-a17b-0ff7653f4599";
        try(var foreign=account("foreign",other,"demand:read","demand:edit")){assertThrows(IllegalArgumentException.class,()->attach(foreign,parish));attach(foreign,other);assertTrue(foreign.entries().isEmpty());assertTrue(foreign.get("entry:"+id).isNull());}
    }
    @Test void legacyDraftsAreImportedOnceAndNeverOverwriteAnotherUsersChanges()throws Exception{
        String id;
        try(var alice=account("alice",parish,"demand:read","demand:edit")){
            id=alice.draft(null,TestRequests.complete()).path("id").asText();attach(alice,parish);assertEquals("alice-id",alice.get("entry:"+id).path("createdBy").asText());
        }
        try(var bob=account("bob",parish,"demand:read","demand:edit")){attach(bob,parish);bob.draft(id,TestRequests.complete().put("intention","Correction par un autre secrétaire"));}
        try(var alice=account("alice",parish,"demand:read","demand:edit")){
            attach(alice,parish);var entry=alice.get("entry:"+id);assertEquals("Correction par un autre secrétaire",entry.path("payload").path("intention").asText());assertEquals("alice-id",entry.path("createdBy").asText());assertEquals("bob-id",entry.path("updatedBy").asText());assertEquals(1,alice.entries().size());
        }
    }
    @Test void protocolAndCurrentRightsAreRequiredBeforeTransmittingSharedCommands()throws Exception{
        String id;
        try(var writer=account("writer",parish,"demand:read","demand:edit")){attach(writer,parish);id=new LocalRequestService(writer).finalizePayload(null,TestRequests.complete().put("emailFidele","a@example.test")).path("localRequestId").asText();}
        var server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);var version=new java.util.concurrent.atomic.AtomicInteger();var calls=new java.util.concurrent.atomic.AtomicInteger();
        try(var reader=account("reader",parish,"demand:read")){
            attach(reader,parish);var session=reader.get("session");
            server.createContext("/auth/me",e->reply(e,session));server.createContext("/desktop/capabilities",e->reply(e,json.createObjectNode().put("parishJournalVersion",version.get())));server.createContext("/desktop/operations",e->{calls.incrementAndGet();reply(e,json.createObjectNode());});server.start();
            var sync=new SyncService(reader,new com.missanye.desktop.api.ApiClient("http://127.0.0.1:"+server.getAddress().getPort()),parish);
            assertThrows(IllegalArgumentException.class,sync::synchronize);version.set(1);sync.synchronize();assertEquals(0,calls.get());assertEquals("PENDING",reader.get("entry:"+id).path("state").asText());assertEquals(0,reader.get("entry:"+id).path("attempts").asInt());
        }finally{server.stop(0);}
    }
    private static void reply(com.sun.net.httpserver.HttpExchange exchange,JsonNode body)throws java.io.IOException{byte[] bytes=body.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();}

}
