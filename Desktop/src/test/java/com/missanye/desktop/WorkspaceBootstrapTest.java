package com.missanye.desktop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.missanye.desktop.repository.LocalStore;
import com.missanye.desktop.service.WorkspaceBootstrap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class WorkspaceBootstrapTest {
    @TempDir Path temp;
    private final ObjectMapper json=new ObjectMapper();
    private final String address="https://example.com/", parish="731c7ab4-2fae-4d78-a17b-0ff7653f4501";
    @Test void firstPreparationIsAtomicAndSurvivesCaseInsensitiveLogin()throws Exception{
        Path file=temp.resolve("workspace.sqlite");
        assertEquals(LocalStore.ProfileStatus.ABSENT,LocalStore.inspect(file,address,"Alice"));assertFalse(Files.exists(file));
        var session=json.readTree("{\"user\":{\"id\":\"owner\"}}");
        var parishes=json.createArrayNode().addObject().put("id",parish).put("nom","Saint Paul");
        var allowed=json.createArrayNode().add(parishes);
        var catalogue=json.readTree("{\"types\":[],\"horaires\":[],\"paiements\":[],\"forfaits\":{}}");
        try(var store=LocalStore.unlock(file,address,"Alice","Local password 1234!".toCharArray(),"owner")){
            assertEquals(LocalStore.ProfileStatus.NEEDS_ONLINE,LocalStore.inspect(file,address,"alice"));
            assertThrows(IllegalArgumentException.class,()->store.installWorkspace(session,allowed,parish,new WorkspaceBootstrap.Prepared(session,catalogue,json.nullNode())));
            assertFalse(store.workspaceReady(parish));assertTrue(store.get("session").isNull());
            store.installWorkspace(session,allowed,parish,new WorkspaceBootstrap.Prepared(session,catalogue,json.createArrayNode()));
            assertTrue(store.workspaceReady(parish));assertEquals(parish,store.get("selected-parish").asText());
        }
        assertEquals(LocalStore.ProfileStatus.READY,LocalStore.inspect(file,address,"ALICE"));
        try(var reopened=LocalStore.unlock(file,address,"ALICE","Local password 1234!".toCharArray(),null)){
            assertTrue(reopened.workspaceReady(parish));assertEquals("owner",reopened.get("ready:"+parish).path("owner").asText());
        }
    }
    @Test void inactiveDeletedSystemAndInvalidTenantsAreExcluded()throws Exception{
        var tenants=json.readTree("""
          [{"id":"731c7ab4-2fae-4d78-a17b-0ff7653f4501","active":true,"statut":"EN_TOLERANCE"},
           {"id":"731c7ab4-2fae-4d78-a17b-0ff7653f4502","active":false},
           {"id":"731c7ab4-2fae-4d78-a17b-0ff7653f4503","isSystem":true},
           {"id":"731c7ab4-2fae-4d78-a17b-0ff7653f4504","statusDel":true},{"id":"bad"}]
          """);
        assertEquals(1,WorkspaceBootstrap.eligible(tenants).size());assertTrue(WorkspaceBootstrap.allows(tenants,parish));
    }
}
