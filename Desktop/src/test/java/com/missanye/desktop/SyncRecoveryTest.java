package com.missanye.desktop;

import com.fasterxml.jackson.databind.*;
import com.missanye.desktop.api.ApiClient;
import com.missanye.desktop.repository.LocalStore;
import com.missanye.desktop.service.SyncService;
import com.sun.net.httpserver.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class SyncRecoveryTest {
    @TempDir Path temp;
    private final ObjectMapper json=new ObjectMapper();
    private LocalStore store()throws Exception{return LocalStore.unlock(temp.resolve("store.sqlite"),"https://api.example/","alice","Local password 1234!".toCharArray(),"alice-id");}
    private static void reply(HttpExchange e,int status,String text)throws java.io.IOException{byte[] body=text.getBytes(java.nio.charset.StandardCharsets.UTF_8);e.sendResponseHeaders(status,body.length);e.getResponseBody().write(body);e.close();}
    private static ApiClient client(HttpServer server){return new ApiClient("http://127.0.0.1:"+server.getAddress().getPort());}
    private static HttpServer server()throws Exception{var s=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);s.createContext("/auth/me",e->reply(e,200,"{\"user\":{\"id\":\"alice-id\",\"permissions\":[\"demand:read\"]}}"));return s;}
    @Test void forbiddenOperationDoesNotBlockOthersAndRetryUsesSameId()throws Exception{
        var server=server();var forbidden=new AtomicReference<String>();var allow=new AtomicBoolean();var accepted=new AtomicInteger();
        server.createContext("/desktop/operations",e->{var body=json.readTree(e.getRequestBody());String id=body.path("operationId").asText();if(id.equals(forbidden.get())&&!allow.get()){reply(e,403,"{}");return;}accepted.incrementAndGet();reply(e,200,"{\"publicId\":\""+id+"\",\"codeSuivie\":\"MS-123\",\"intention\":\"Demande confirmée\",\"paroissePublicId\":\""+TestRequests.PARISH+"\"}");});server.start();
        try(var store=store()){
            var one=store.draft(null,TestRequests.complete());forbidden.set(one.path("id").asText());store.queue(forbidden.get());
            var two=store.draft(null,TestRequests.complete());store.queue(two.path("id").asText());var sync=new SyncService(store,client(server));sync.synchronize();
            assertEquals("BLOCKED",store.get("entry:"+forbidden.get()).path("state").asText());assertEquals("SENT",store.get("entry:"+two.path("id").asText()).path("state").asText());assertEquals(1,accepted.get());
            var before=store.get("entry:"+forbidden.get());allow.set(true);store.retryBlocked(forbidden.get());sync.synchronize();var after=store.get("entry:"+forbidden.get());assertEquals(before.path("payload"),after.path("payload"));assertEquals("SENT",after.path("state").asText());assertEquals(2,accepted.get());
            assertEquals("demand:read",store.get("session").path("user").path("permissions").path(0).asText());
        }finally{server.stop(0);}
    }
    @Test void cashConfirmationRemainsVisibleWhenFollowingRefreshFails()throws Exception{
        var server=server();server.createContext("/desktop/operations",e->reply(e,200,"{\"publicId\":\"demand-id\",\"intention\":\"Intention\",\"montant\":5000,\"statutPaiement\":\"PAYE\",\"paroissePublicId\":\""+TestRequests.PARISH+"\"}"));server.createContext("/demandes/paroisse/",e->reply(e,503,"{}"));server.start();
        try(var store=store()){
            var demand=json.createObjectNode().put("publicId","demand-id").put("montant",5000).put("paroissePublicId",TestRequests.PARISH).put("statutPaiement","NON_PAYE");store.put("demands:"+TestRequests.PARISH,json.createArrayNode().add(demand));
            var cash=store.queueCash(demand);var sync=new SyncService(store,client(server));sync.synchronize();assertThrows(ApiClient.ApiException.class,()->sync.refresh(TestRequests.PARISH));
            assertEquals("SENT",store.get("entry:"+cash.path("id").asText()).path("state").asText());assertEquals("PAYE",store.get("demands:"+TestRequests.PARISH).path(0).path("statutPaiement").asText());
        }finally{server.stop(0);}
    }
    @Test void unavailablePdfCanUseRetainedCopyButMissingCopyIsExplicit()throws Exception{
        var server=server();server.createContext("/demandes/code/",e->reply(e,503,"{}"));server.start();
        try(var store=store()){
            byte[] pdf="%PDF-retained-copy".getBytes();store.put("receipt:MS-123",json.getNodeFactory().binaryNode(pdf));var sync=new SyncService(store,client(server));var fallback=sync.receiptDownload("MS-123",true);assertTrue(fallback.cached());assertArrayEquals(pdf,fallback.bytes());assertThrows(IllegalArgumentException.class,()->sync.receiptDownload("MS-unknown",true));
        }finally{server.stop(0);}
    }
    @Test void successStatusWithoutValidConfirmationKeepsOriginalOperationPending()throws Exception{
        var server=server();var body=new AtomicReference<>("{}");server.createContext("/desktop/operations",e->reply(e,200,body.get()));server.start();
        try(var store=store()){
            var entry=store.draft(null,TestRequests.complete());String id=entry.path("id").asText();store.queue(id);var sync=new SyncService(store,client(server));
            assertThrows(java.io.IOException.class,sync::synchronize);assertEquals("PENDING",store.get("entry:"+id).path("state").asText());
            body.set(json.createObjectNode().put("publicId","demand-id").put("codeSuivie","MS-123").put("paroissePublicId",TestRequests.PARISH).put("intention","Demande confirmée").toString());
            sync.synchronize();assertEquals("SENT",store.get("entry:"+id).path("state").asText());assertEquals(2,store.get("entry:"+id).path("attempts").asInt());
        }finally{server.stop(0);}
    }
    @Test void cashConfirmationForWrongDemandOrAmountIsNotAccepted()throws Exception{
        var server=server();var response=new AtomicReference<>(json.createObjectNode().put("publicId","another-demand").put("statutPaiement","PAYE").put("montant",5000));server.createContext("/desktop/operations",e->reply(e,200,response.get().toString()));server.start();
        try(var store=store()){
            var entry=store.queueCash(json.createObjectNode().put("publicId","demand-id").put("montant",5000));var sync=new SyncService(store,client(server));
            assertThrows(java.io.IOException.class,sync::synchronize);response.get().put("publicId","demand-id").put("montant",6000);assertThrows(java.io.IOException.class,sync::synchronize);
            assertEquals("PENDING",store.get("entry:"+entry.path("id").asText()).path("state").asText());
        }finally{server.stop(0);}
    }

}
