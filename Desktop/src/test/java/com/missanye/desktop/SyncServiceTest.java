package com.missanye.desktop;

import com.fasterxml.jackson.databind.*;
import com.missanye.desktop.api.ApiClient;
import com.missanye.desktop.repository.LocalStore;
import com.missanye.desktop.service.SyncService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class SyncServiceTest {
    @TempDir Path temp;
    private final ObjectMapper json=new ObjectMapper();
    private LocalStore open(String owner)throws Exception{return LocalStore.unlock(temp.resolve("vault.sqlite"),"https://test/","alice","Local password 1234!".toCharArray(),owner);}
    private static void reply(com.sun.net.httpserver.HttpExchange e,int code,String text)throws java.io.IOException{byte[] bytes=text.getBytes();e.sendResponseHeaders(code,bytes.length);e.getResponseBody().write(bytes);e.close();}
    @Test void uncertainResponseRetriesSameIdAfterReopen()throws Exception{
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);var attempts=new AtomicInteger();var firstId=new AtomicReference<String>();var sameId=new AtomicBoolean(true);
        server.createContext("/auth/me",e->reply(e,200,"{\"user\":{\"id\":\"alice-id\"}}"));
        server.createContext("/desktop/operations",e->{String body=new String(e.getRequestBody().readAllBytes());String id=json.readTree(body).path("operationId").asText();if(firstId.get()==null)firstId.set(id);else sameId.set(firstId.get().equals(id));if(attempts.incrementAndGet()==1)reply(e,503,"{}");else reply(e,200,"{\"publicId\":\"server-id\",\"codeSuivie\":\"MS-123\"}");});server.start();
        try{
            var client=new ApiClient("http://127.0.0.1:"+server.getAddress().getPort());
            try(var store=open("alice-id")){var entry=store.draft(null,json.createObjectNode().put("intention","Une intention locale"));store.queue(entry.path("id").asText());assertThrows(ApiClient.ApiException.class,()->new SyncService(store,client).synchronize());assertEquals("PENDING",store.entries().get(0).path("state").asText());}
            try(var store=open(null)){new SyncService(store,client).synchronize();assertEquals("SENT",store.entries().get(0).path("state").asText());new SyncService(store,client).synchronize();assertEquals(2,attempts.get());assertTrue(sameId.get());}
        }finally{server.stop(0);}
    }
    @Test void wrongSessionCannotSendAndExpiredSessionLeavesPending()throws Exception{
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);var expired=new AtomicBoolean();var sent=new AtomicBoolean();
        server.createContext("/auth/me",e->{if(expired.get())reply(e,401,"{}");else reply(e,200,"{\"user\":{\"id\":\"someone-else\"}}");});server.createContext("/desktop/operations",e->{sent.set(true);reply(e,200,"{}");});server.start();
        try(var store=open("alice-id")){var d=store.draft(null,json.createObjectNode());store.queue(d.path("id").asText());var sync=new SyncService(store,new ApiClient("http://127.0.0.1:"+server.getAddress().getPort()));assertThrows(IllegalArgumentException.class,sync::synchronize);expired.set(true);assertThrows(ApiClient.ApiException.class,sync::synchronize);assertFalse(sent.get());assertEquals("PENDING",store.entries().get(0).path("state").asText());}finally{server.stop(0);}
    }
    @Test void validationRejectionIsRetainedAndCanBeCorrectedAsNewDraft()throws Exception{
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.createContext("/auth/me",e->reply(e,200,"{\"user\":{\"id\":\"alice-id\"}}"));server.createContext("/desktop/operations",e->reply(e,409,"{\"message\":\"Date trop proche\"}"));server.start();
        try(var store=open("alice-id")){var d=store.draft(null,json.createObjectNode().put("intention","Intention"));store.queue(d.path("id").asText());new SyncService(store,new ApiClient("http://127.0.0.1:"+server.getAddress().getPort())).synchronize();assertEquals("REJECTED",store.entries().get(0).path("state").asText());assertTrue(store.entries().get(0).path("error").asText().contains("Date trop proche"));var replacement=store.draft(null,d.path("payload"));assertNotEquals(d.path("id").asText(),replacement.path("id").asText());}finally{server.stop(0);}
    }
}
