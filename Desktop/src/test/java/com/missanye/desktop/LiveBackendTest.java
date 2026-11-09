package com.missanye.desktop;

import com.fasterxml.jackson.databind.*;
import com.missanye.desktop.api.ApiClient;
import com.missanye.desktop.repository.LocalStore;
import com.missanye.desktop.service.SyncService;
import com.missanye.desktop.service.WorkspaceBootstrap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/** Only against the disposable database populated by ApplicationStartupSmokeTest. */
@EnabledIfEnvironmentVariable(named="DESKTOP_E2E",matches="true")
class LiveBackendTest {
    @TempDir Path temp;
    @Test void realBackendLoginOfflineCreateCashAndPdf()throws Exception{
        var api=new ApiClient(System.getenv().getOrDefault("DESKTOP_API_URL","http://127.0.0.1:18081"));
        var session=api.login("ci-platform-admin","CiStrongPassword-2026!");String owner=session.path("user").path("id").asText();assertFalse(owner.isBlank());
        var parishes=api.get("paroisses");JsonNode selected=null;
        for(var p:parishes)if(!p.path("isSystem").asBoolean() && p.path("isActive").asBoolean()){selected=p;break;}
        assertNotNull(selected);String parish=selected.path("publicId").asText();
        String id,code;
        try(var store=LocalStore.unlock(temp.resolve("live.sqlite"),api.baseUrl(),"ci-platform-admin","Offline test password!".toCharArray(),owner)){
            var prepared=WorkspaceBootstrap.fetch(api,session,parish);store.installWorkspace(session,WorkspaceBootstrap.eligible(parishes),parish,prepared);assertTrue(store.workspaceReady(parish));
            var rows=prepared.demands();assertFalse(rows.isEmpty());JsonNode source=rows.get(0);
            var p=new ObjectMapper().createObjectNode().put("intention","Demande desktop de bout en bout").put("telFidele","+22890123458").put("nomFidele","Test desktop").put("prenomFidele","CI");
            for(String field:new String[]{"paroissePublicId","typeDemandePublicId","forfaitTarifPublicId","horairePublicId","typePaiementPublicId"})p.set(field,source.get(field));
            p.put("dateDebut",source.path("datesCelebration").get(0).asText());
            var draft=store.draft(null,p);id=draft.path("id").asText();store.queue(id);
        }
        try(var store=LocalStore.unlock(temp.resolve("live.sqlite"),api.baseUrl(),"ci-platform-admin","Offline test password!".toCharArray(),null)){
            var sync=new SyncService(store,api);sync.synchronize();var created=store.get("entry:"+id);assertEquals("SENT",created.path("state").asText());var result=created.path("result");code=result.path("codeSuivie").asText();assertFalse(code.isBlank());
            var cash=store.queueCash(result);sync.synchronize();assertEquals("PAYE",store.get("entry:"+cash.path("id").asText()).path("result").path("statutPaiement").asText());
            JsonNode current=api.get("demandes/"+result.path("publicId").asText());
            var correction=store.queueMutation("UPDATE_INTENTION",current,"Intention corrigée depuis le desktop",null);
            sync.synchronize();assertEquals("SENT",store.get("entry:"+correction.path("id").asText()).path("state").asText());
            JsonNode latest=api.get("demandes/"+result.path("publicId").asText());
            var conflicting=store.queueMutation("UPDATE_INTENTION",latest,"Cette modification locale deviendra en conflit",null);
            var competing=new ObjectMapper().createObjectNode().put("operationId",java.util.UUID.randomUUID().toString()).put("kind","UPDATE_INTENTION");
            competing.putObject("payload").put("demandePublicId",latest.path("publicId").asText()).put("expectedUpdatedAt",latest.path("updatedAt").asText()).put("intention","Modification concurrente sur un autre poste");
            api.post("desktop/operations",competing);sync.synchronize();
            assertEquals("REJECTED",store.get("entry:"+conflicting.path("id").asText()).path("state").asText());
            assertEquals("Modification concurrente sur un autre poste",api.get("demandes/"+result.path("publicId").asText()).path("intention").asText());
            // Create another unpaid demand, delete it, and replay deletion safely.
            var deletionDraft=store.draft(null,created.path("payload"));store.queue(deletionDraft.path("id").asText());sync.synchronize();
            JsonNode toDelete=api.get("demandes/"+store.get("entry:"+deletionDraft.path("id").asText()).path("result").path("publicId").asText());
            var deletion=store.queueMutation("DELETE_DEMANDE",toDelete,null,null);sync.synchronize();
            var deletionResult=store.get("entry:"+deletion.path("id").asText());assertEquals("SENT",deletionResult.path("state").asText());assertTrue(deletionResult.path("result").path("deleted").asBoolean());
            var replayDelete=new ObjectMapper().createObjectNode().put("operationId",deletion.path("id").asText()).put("kind","DELETE_DEMANDE");replayDelete.set("payload",deletion.path("payload"));
            assertTrue(api.post("desktop/operations",replayDelete).path("deleted").asBoolean());
            for(var row:sync.refresh(parish))assertNotEquals(toDelete.path("publicId").asText(),row.path("publicId").asText());
            var bytes=sync.receipt(code,true);assertEquals("%PDF-",new String(bytes,0,5,java.nio.charset.StandardCharsets.US_ASCII));assertArrayEquals(bytes,sync.receipt(code,false));
        }
        api.logout();
        try(var store=LocalStore.unlock(temp.resolve("live.sqlite"),api.baseUrl(),"ci-platform-admin","Offline test password!".toCharArray(),null)){
            assertTrue(new SyncService(store,null).receipt(code,false).length>100);
        }
    }
}
