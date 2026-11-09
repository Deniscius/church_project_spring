package com.missanye.desktop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.missanye.desktop.repository.LocalStore;
import com.missanye.desktop.model.RequestValidation;
import com.missanye.desktop.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.sql.DriverManager;
import static org.junit.jupiter.api.Assertions.*;

class WorkflowRegressionTest {
    @TempDir Path temp;
    private final ObjectMapper json=new ObjectMapper();
    private LocalStore store()throws Exception{return LocalStore.unlock(temp.resolve("vault.sqlite"),"https://api.example/","alice","Local password 1234!".toCharArray(),"alice-id");}
    @Test void incompleteDraftSurvivesButCannotBeSubmitted()throws Exception{
        String id;
        try(var store=store()){var draft=store.draft(null,json.createObjectNode().put("intention","Travail non terminé"));id=draft.path("id").asText();assertThrows(IllegalArgumentException.class,()->store.queue(id));assertEquals("DRAFT",store.get("entry:"+id).path("state").asText());}
        try(var reopened=store()){assertEquals("Travail non terminé",reopened.get("entry:"+id).path("payload").path("intention").asText());}
    }
    @Test void draftMetadataIsRemovedAndInvalidTimesAreRejected(){
        var payload=TestRequests.complete().put("_customTimeText","06:30").put("_expectedCelebrations",1);payload.putArray("datesCelebration").add("2027-01-10");
        var prepared=RequestValidation.forSubmission(payload);assertEquals("06:30",prepared.path("heurePersonnalisee").asText());assertFalse(prepared.has("_customTimeText"));assertFalse(prepared.has("_expectedCelebrations"));assertFalse(prepared.has("horairePublicId"));
        payload.put("_customTimeText","pas une heure");assertThrows(IllegalArgumentException.class,()->RequestValidation.forSubmission(payload));
    }
    @Test void cashAndCacheConfirmationCommitTogetherAndRollbackTogether()throws Exception{
        try(var store=store()){
            var demand=json.createObjectNode().put("publicId","demand-id").put("paroissePublicId",TestRequests.PARISH).put("intention","Intention originale").put("montant",5000).put("statutPaiement","NON_PAYE");
            store.put("demands:"+TestRequests.PARISH,json.createArrayNode().add(demand));var cash=store.queueCash(demand);
            var paid=demand.deepCopy().put("statutPaiement","PAYE");
            try(var connection=DriverManager.getConnection("jdbc:sqlite:"+temp.resolve("vault.sqlite"));var sql=connection.createStatement()){
                sql.execute("CREATE TRIGGER fail_cache BEFORE UPDATE ON vault_data WHEN NEW.name LIKE 'demands:%' BEGIN SELECT RAISE(ABORT,'injected failure'); END");
                assertThrows(Exception.class,()->store.confirm(cash,paid));
                assertEquals("PENDING",store.get("entry:"+cash.path("id").asText()).path("state").asText());assertEquals("NON_PAYE",store.get("demands:"+TestRequests.PARISH).path(0).path("statutPaiement").asText());
                sql.execute("DROP TRIGGER fail_cache");
            }
            store.confirm(cash,paid);assertEquals("SENT",store.get("entry:"+cash.path("id").asText()).path("state").asText());assertEquals("PAYE",store.get("demands:"+TestRequests.PARISH).path(0).path("statutPaiement").asText());
        }
    }
    @Test void cashKeepsItsParishAndRejectsFractionalAmountsOrPendingChanges()throws Exception{
        try(var store=store()){
            var demand=json.createObjectNode().put("publicId","demand-id").put("updatedAt","2027-01-01T12:00:00").put("paroissePublicId",TestRequests.PARISH).put("montant",5000);
            var invalid=demand.deepCopy().put("montant",5000.5);assertThrows(IllegalArgumentException.class,()->store.queueCash(invalid));
            store.queueMutation("DELETE_DEMANDE",demand,null,null);assertThrows(IllegalArgumentException.class,()->store.queueCash(demand));
        }
        try(var other=LocalStore.unlock(temp.resolve("cash.sqlite"),"https://api.example/","alice","Local password 1234!".toCharArray(),"alice-id")){
            var demand=json.createObjectNode().put("publicId","other").put("paroissePublicId",TestRequests.PARISH).put("montant",5000);assertEquals(TestRequests.PARISH,other.queueCash(demand).path("parish").asText());
        }
    }
    @Test void retryingBlockedOperationKeepsItsOriginalIdentityAndPayload()throws Exception{
        try(var store=store()){var draft=store.draft(null,TestRequests.complete());String id=draft.path("id").asText();store.queue(id);var blocked=(com.fasterxml.jackson.databind.node.ObjectNode)store.get("entry:"+id);blocked.put("state","BLOCKED").put("attempts",1);store.put("entry:"+id,blocked);store.retryBlocked(id);var retried=store.get("entry:"+id);assertEquals(id,retried.path("id").asText());assertEquals(blocked.path("payload"),retried.path("payload"));assertEquals(1,retried.path("attempts").asInt());}
    }
    @Test void applicationLockCanOnlyBeOwnedByOneProcessAndCanBeReopened()throws Exception{
        try(var first=ApplicationLock.acquire(temp)){assertThrows(ApplicationLock.AlreadyRunningException.class,()->ApplicationLock.acquire(temp));}
        try(var afterRestart=ApplicationLock.acquire(temp)){assertNotNull(afterRestart);}
    }
    @Test void pdfExportDoesNotOverwriteOriginalOnInvalidResponse()throws Exception{
        Path target=temp.resolve("receipt.pdf");byte[] original="%PDF-previous".getBytes();Files.write(target,original);
        assertThrows(IllegalArgumentException.class,()->ReceiptExporter.write(target,"<html>failure</html>".getBytes()));assertArrayEquals(original,Files.readAllBytes(target));
        byte[] replacement="%PDF-new document".getBytes();ReceiptExporter.write(target,replacement);assertArrayEquals(replacement,Files.readAllBytes(target));
        try(var files=Files.list(temp)){assertEquals(1,files.count());}
    }
    @Test void correctedRejectionHasOneSuccessorAndCannotBeCorrectedTwice()throws Exception{
        try(var store=store()){
            var original=store.draft(null,TestRequests.complete());store.queue(original.path("id").asText());var rejected=(com.fasterxml.jackson.databind.node.ObjectNode)store.get("entry:"+original.path("id").asText());rejected.put("state","REJECTED").put("error","Date à corriger");store.put("entry:"+original.path("id").asText(),rejected);
            var corrected=store.correctDraft(original.path("id").asText(),TestRequests.complete());var prior=store.get("entry:"+original.path("id").asText());
            assertEquals("SUPERSEDED",prior.path("state").asText());assertEquals(corrected.path("id").asText(),prior.path("replacedBy").asText());assertEquals("Date à corriger",prior.path("error").asText());
            assertThrows(IllegalArgumentException.class,()->store.correctDraft(original.path("id").asText(),TestRequests.complete()));assertEquals(2,store.entries().size());
        }
    }

    @Test void onlyUnattemptedRequestsCanReturnToDraft()throws Exception{
        try(var store=store()){
            var draft=store.draft(null,TestRequests.complete());String id=draft.path("id").asText();store.queue(id);store.returnToDraft(id);assertEquals("DRAFT",store.get("entry:"+id).path("state").asText());
            store.draft(id,TestRequests.complete().put("intention","Intention corrigée avant le premier envoi"));store.queue(id);
            var attempted=(com.fasterxml.jackson.databind.node.ObjectNode)store.get("entry:"+id);attempted.put("attempts",1);store.put("entry:"+id,attempted);assertThrows(IllegalArgumentException.class,()->store.returnToDraft(id));
            var cash=store.queueCash(json.createObjectNode().put("publicId","cash-demand").put("montant",5000));assertThrows(IllegalArgumentException.class,()->store.returnToDraft(cash.path("id").asText()));
        }
    }

}
