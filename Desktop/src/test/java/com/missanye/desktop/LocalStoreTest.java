package com.missanye.desktop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.missanye.desktop.repository.LocalStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class LocalStoreTest {
    @TempDir Path temp;
    private final ObjectMapper json=new ObjectMapper();
    private LocalStore open(String owner)throws Exception{return LocalStore.unlock(temp.resolve("vault.sqlite"),"https://api.example/","alice","Local password 1234!".toCharArray(),owner);}
    @Test void encryptedDataAndQueuedUuidSurviveRestart()throws Exception{
        String id;
        try(var store=open("alice-id")){
            var draft=store.draft(null,json.readTree("{\"intention\":\"CONFIDENTIAL INTENTION\"}"));id=draft.path("id").asText();store.queue(id);
            assertThrows(IllegalArgumentException.class,()->store.draft(id,json.createObjectNode()));
        }
        String raw=new String(Files.readAllBytes(temp.resolve("vault.sqlite")),java.nio.charset.StandardCharsets.ISO_8859_1);
        assertFalse(raw.contains("CONFIDENTIAL INTENTION"));
        try(var store=open(null)){assertEquals(id,store.entries().get(0).path("id").asText());assertEquals("PENDING",store.entries().get(0).path("state").asText());assertEquals("CONFIDENTIAL INTENTION",store.entries().get(0).path("payload").path("intention").asText());}
    }
    @Test void wrongPasswordDifferentOwnerAndDifferentServerCannotReadVault()throws Exception{
        try(var store=open("alice-id")){store.put("cached",json.readTree("{\"secret\":42}"));}
        assertThrows(IllegalArgumentException.class,()->LocalStore.unlock(temp.resolve("vault.sqlite"),"https://api.example/","alice","wrong password!!!".toCharArray(),null));
        assertThrows(IllegalArgumentException.class,()->open("different-owner"));
        assertThrows(IllegalArgumentException.class,()->LocalStore.unlock(temp.resolve("vault.sqlite"),"https://other.example/","alice","Local password 1234!".toCharArray(),null));
        try(var other=LocalStore.unlock(temp.resolve("vault.sqlite"),"https://api.example/","bob","Local password 1234!".toCharArray(),"bob-id")){assertTrue(other.get("cached").isNull());}
    }
    @Test void duplicateCashCannotBeQueuedAndPendingCannotBeArchived()throws Exception{
        try(var store=open("alice-id")){
            var demand=json.readTree("{\"publicId\":\"123\",\"montant\":5000,\"codeSuivie\":\"MS-123\"}");
            var entry=store.queueCash(demand);assertThrows(IllegalArgumentException.class,()->store.queueCash(demand));
            assertThrows(IllegalArgumentException.class,()->store.archive(entry.path("id").asText()));
        }
    }
}
