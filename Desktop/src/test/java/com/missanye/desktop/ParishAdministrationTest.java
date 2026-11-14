package com.missanye.desktop;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.missanye.desktop.admin.*;
import com.missanye.desktop.admin.ParishModules.Module;
import com.missanye.desktop.api.ApiClient;
import com.missanye.desktop.repository.LocalStore;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class ParishAdministrationTest {
    @TempDir Path directory;
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final String PARISH=TestRequests.PARISH;
    private Module module(String id){return ParishModules.all().stream().filter(m->m.id().equals(id)).findFirst().orElseThrow();}
    private ObjectNode session(String... permissions){var value=JSON.createObjectNode();value.putObject("user").put("id","owner").put("isGlobal",false).putArray("permissions").addAll(java.util.Arrays.stream(permissions).map(JSON.getNodeFactory()::textNode).toList());value.putArray("paroisses").addObject().put("id",PARISH).put("active",true);return value;}
    @Test void offlineConfigurationIsReadOnlyAndCacheIsScopedToAccountAndParish()throws Exception{
        try(var store=LocalStore.unlock(directory.resolve("offline.sqlite"),"http://localhost:8081/","alice","Local password 1234!".toCharArray(),"owner")){
            var service=new ParishAdministration(null,store,PARISH,session("schedule:read","schedule:manage"));
            var cached=JSON.createArrayNode();cached.addObject().put("libelle","Matin");store.put("administration:owner:"+PARISH+":schedules",cached);assertEquals("Matin",service.load(module("schedules"),java.time.LocalDate.now()).path(0).path("libelle").asText());
            assertTrue(service.modules().stream().anyMatch(m->m.id().equals("schedules")));
            assertThrows(IllegalArgumentException.class,()->service.save(module("schedules"),JSON.createObjectNode(),null));
            assertThrows(IllegalArgumentException.class,()->service.load(module("team"),java.time.LocalDate.now()));
            var other=session("schedule:read");((ObjectNode)other.path("user")).put("id","another-user");
            assertThrows(IllegalArgumentException.class,()->new ParishAdministration(null,store,PARISH,other).load(module("schedules"),java.time.LocalDate.now()));
        }
    }
    @Test void writesRecheckLiveIdentityAndPermissionAndSupportPutPatchAndEmptyDelete()throws Exception{
        var current=new AtomicReference<JsonNode>(session("schedule:read","schedule:manage","parish:read","parish-settings:manage"));var method=new AtomicReference<String>();var body=new AtomicReference<JsonNode>();var calls=new AtomicInteger();
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/auth/me",e->{byte[] bytes=current.get().toString().getBytes(StandardCharsets.UTF_8);e.sendResponseHeaders(200,bytes.length);e.getResponseBody().write(bytes);e.close();});
        server.createContext("/",e->{method.set(e.getRequestMethod());byte[] bytes=e.getRequestBody().readAllBytes();body.set(bytes.length==0?JSON.nullNode():JSON.readTree(bytes));calls.incrementAndGet();e.sendResponseHeaders(204,-1);e.close();});server.start();
        try(var store=LocalStore.unlock(directory.resolve("api.sqlite"),"http://localhost:8081/","alice","Local password 1234!".toCharArray(),"owner")){
            var api=new ApiClient("http://127.0.0.1:"+server.getAddress().getPort()+"/");var service=new ParishAdministration(api,store,PARISH,current.get());
            var schedule=JSON.createObjectNode().put("jourSemaine","LUNDI").put("heureCelebration","07:30").put("isActive",true);var selected=JSON.createObjectNode().put("publicId","731c7ab4-2fae-4d78-a17b-0ff7653f4502");
            service.save(module("schedules"),schedule,selected);assertEquals("PUT",method.get());assertEquals(PARISH,body.get().path("paroissePublicId").asText());assertTrue(body.get().path("natureHonoraire").isNull());
            service.save(module("parish"),JSON.createObjectNode().put("telephone","+22890123456"),JSON.createObjectNode());assertEquals("PATCH",method.get());
            service.remove(module("schedules"),selected);assertEquals("DELETE",method.get());assertTrue(body.get().isNull());int before=calls.get();
            var changed=session("schedule:read","schedule:manage");((ObjectNode)changed.path("user")).put("id","intruder");current.set(changed);assertThrows(IllegalArgumentException.class,()->service.save(module("schedules"),schedule,selected));assertEquals(before,calls.get());
            current.set(session("schedule:read"));assertThrows(IllegalArgumentException.class,()->service.save(module("schedules"),schedule,selected));assertEquals(before,calls.get());api.clear();
        }finally{server.stop(0);}
    }
    @Test void teamEditingCannotResetIdentityOrElevateToPlatformAndPasswordsStayOutOfPayload()throws Exception{
        var service=new ParishAdministration(null,null,PARISH,session("user:manage"));
        var existing=JSON.createObjectNode().put("publicId","another-user").put("nom","Original").put("prenom","Name").put("username","original").put("email","original@example.test");
        var malicious=JSON.createObjectNode().put("nom","Changed").put("role","SECRETAIRE").put("isGlobal",true).put("isActive",true).put("password","reset12345");
        var body=service.payload(module("team"),malicious,existing);assertEquals("Original",body.path("nom").asText());assertFalse(body.path("isGlobal").asBoolean());assertFalse(body.has("password"));
        malicious.put("role","SUPER_ADMIN");assertThrows(IllegalArgumentException.class,()->service.payload(module("team"),malicious,existing));
        var password=JSON.createObjectNode().put("currentPassword","old").put("newPassword","new password").put("confirmPassword","different");assertThrows(IllegalArgumentException.class,()->service.payload(module("password"),password,null));
    }
    @Test void bankAndTariffValidationMirrorWebRules(){
        assertEquals("TG0090600000000000000000",BankDetails.validate("TG 00906 00000000000000000","Ecobank Togo"));
        assertThrows(IllegalArgumentException.class,()->BankDetails.validate("TG0090600000000000000000","Orabank Togo"));
        assertThrows(IllegalArgumentException.class,()->BankDetails.validate("TG00TG0090600000000000000000","Ecobank Togo"));
        var service=new ParishAdministration(null,null,PARISH,session("pricing:manage"));var price=JSON.createObjectNode().put("montantForfait",2000).put("nombreCelebration",1).put("natureForfait","NORMALE").put("heurePersonnalise",true);price.putArray("joursCelebrationAutorises").add("LUNDI");assertThrows(IllegalArgumentException.class,()->service.payload(module("prices"),price,null));
    }
}
