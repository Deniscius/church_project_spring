package com.missanye.desktop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.missanye.desktop.controller.WorkbenchController;
import com.missanye.desktop.repository.LocalStore;
import com.missanye.desktop.view.WorkbenchView;
import javafx.application.Platform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import static org.junit.jupiter.api.Assertions.*;

class MainControllerTest {
    @TempDir Path temp;
    private static void fx(Runnable action)throws Exception{var task=new FutureTask<Void>(action,null);Platform.runLater(task);task.get(10,TimeUnit.SECONDS);}
    private static void await(BooleanSupplier condition)throws Exception{
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);var result=new java.util.concurrent.atomic.AtomicBoolean();
        while(!result.get() && System.nanoTime()<deadline){fx(()->result.set(condition.getAsBoolean()));if(!result.get())Thread.sleep(20);}assertTrue(result.get());
    }
    private static void capture(WorkbenchView view,String name){
        view.root.applyCss();view.root.layout();
        var image=view.root.snapshot(null,null);
        var buffered=new java.awt.image.BufferedImage((int)image.getWidth(),(int)image.getHeight(),java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<buffered.getHeight();y++)for(int x=0;x<buffered.getWidth();x++)buffered.setRGB(x,y,image.getPixelReader().getArgb(x,y));
        try{javax.imageio.ImageIO.write(buffered,"png",Path.of("target",name).toFile());}catch(java.io.IOException e){throw new RuntimeException(e);}
    }
    @Test void offlineUnlockSaveAndLockUseRealEncryptedSqlite()throws Exception{
        System.setProperty("glass.platform","Monocle");System.setProperty("monocle.platform","Headless");System.setProperty("prism.order","sw");
        System.setProperty("headless.geometry","1600x1200-32");
        var started=new CountDownLatch(1);Platform.startup(started::countDown);assertTrue(started.await(10,TimeUnit.SECONDS));Platform.setImplicitExit(false);
        Path file=temp.resolve("ui.sqlite");var json=new ObjectMapper();String parish="731c7ab4-2fae-4d78-a17b-0ff7653f4501";
        try(var store=LocalStore.unlock(file,"http://localhost:8081/","alice","Local password 1234!".toCharArray(),"alice-id")){
            store.put("session",json.readTree("{\"user\":{\"id\":\"alice-id\",\"username\":\"alice\",\"permissions\":[\"demand:edit\",\"payment:manage\"]}}"));
            store.put("parishes",json.readTree("[{\"id\":\""+parish+"\",\"nom\":\"Saint Paul\"}]"));
            store.put("catalogue:"+parish,json.readTree("{\"types\":[{\"publicId\":\"type-id\",\"libelle\":\"Normale\"}],\"forfaits\":{\"type-id\":[{\"publicId\":\"plan-id\",\"nomForfait\":\"Un jour\",\"nombreCelebration\":1,\"montantForfait\":2000}]},\"horaires\":[{\"publicId\":\"schedule-id\",\"libelle\":\"Matin\"}],\"paiements\":[{\"publicId\":\"cash-id\",\"libelle\":\"Espèces\"}]}"));
        }
        try(var store=LocalStore.unlock(file,"http://localhost:8081/","alice","Local password 1234!".toCharArray(),null)){
            store.put("demands:"+parish,json.readTree("""
              [
                {"publicId":"demo-1","codeSuivie":"MS-DEMO-001","prenomFidele":"Claire","nomFidele":"Mensah","intention":"Action de grâce pour la famille","datesCelebration":["2027-01-10"],"montant":5000,"statutPaiement":"PAYE","statutValidation":"VALIDEE"},
                {"publicId":"demo-2","codeSuivie":"MS-DEMO-002","prenomFidele":"Ama","nomFidele":"Amega","intention":"Intention pour un anniversaire","datesCelebration":["2027-01-11"],"montant":5000,"statutPaiement":"NON_PAYE","statutValidation":"VALIDEE"},
                {"publicId":"demo-3","codeSuivie":"MS-DEMO-003","prenomFidele":"Pierre","nomFidele":"Dossou","intention":"Prière pour un proche","datesCelebration":["2027-01-12"],"montant":20000,"statutPaiement":"NON_PAYE","statutValidation":"EN_ATTENTE","validationRequise":true}
              ]
              """));
        }
        var views=new WorkbenchView[1];var controllers=new WorkbenchController[1];var stage=new javafx.stage.Stage[1];
        try{
            fx(()->{views[0]=new WorkbenchView();controllers[0]=new WorkbenchController(views[0],file,null,"http://localhost:8081");stage[0]=new javafx.stage.Stage();stage[0].setScene(new javafx.scene.Scene(views[0].root,1200,850));stage[0].show();assertNull(views[0].root.lookup("#server"));capture(views[0],"desktop-login-preview.png");views[0].username.setText("alice");views[0].localPassword.setText("Local password 1234!");views[0].offline.fire();});
            await(()->!views[0].workspace.isDisabled() && views[0].plan.getValue()!=null);
            fx(()->{
                assertEquals(3,views[0].demands.getItems().size());assertEquals("3",views[0].demandCount.getText());
                views[0].demandSearch.setText("AMEGA");assertEquals(1,views[0].demands.getItems().size());
                views[0].demands.getSelectionModel().selectFirst();assertFalse(views[0].cash.isDisabled());
                views[0].demandSearch.clear();views[0].demands.getSelectionModel().selectFirst();assertTrue(views[0].cash.isDisabled());
                views[0].tabs.getSelectionModel().select(1);capture(views[0],"desktop-demands-preview.png");
                views[0].tabs.getSelectionModel().select(0);
                views[0].additionalDate.setValue(views[0].date.getValue().plusDays(1));views[0].addDate.fire();assertEquals(1,views[0].extraDates.getItems().size());
                views[0].extraDates.getSelectionModel().selectFirst();views[0].removeDate.fire();assertTrue(views[0].extraDates.getItems().isEmpty());
            });
            fx(()->{views[0].phone.setText("+22890123456");views[0].intention.setText("Intention enregistrée hors ligne");capture(views[0],"desktop-form-preview.png");views[0].save.fire();});
            await(()->views[0].outbox.getItems().size()==1);
            fx(()->{
                views[0].queueFilter.setValue("À transmettre");assertTrue(views[0].outbox.getItems().isEmpty());
                views[0].queueFilter.setValue("Brouillons");assertEquals(1,views[0].outbox.getItems().size());
                views[0].queueFilter.getSelectionModel().selectFirst();views[0].outbox.getSelectionModel().selectFirst();
                assertFalse(views[0].queue.isDisabled());capture(views[0],"desktop-preview.png");
                stage[0].setWidth(1024);stage[0].setHeight(700);
            });
            await(()->views[0].root.getWidth()<=1040 && views[0].root.getHeight()<=720);
            fx(()->{
                var scroll=(javafx.scene.control.ScrollPane)views[0].root.getCenter();scroll.setVvalue(1);views[0].root.layout();
                capture(views[0],"desktop-compact-preview.png");
                assertTrue(views[0].queue.localToScene(views[0].queue.getBoundsInLocal()).getMaxY()<views[0].root.getHeight());
            });
            try(var reopened=LocalStore.unlock(file,"http://localhost:8081/","alice","Local password 1234!".toCharArray(),null)){assertEquals("DRAFT",reopened.entries().get(0).path("state").asText());}
            fx(()->views[0].lock.fire());await(()->views[0].workspace.isDisabled() && views[0].session.getText().equals("Coffre verrouillé"));
            fx(()->{assertTrue(views[0].outbox.getItems().isEmpty());assertTrue(views[0].intention.getText().isEmpty());});
        }finally{fx(()->{if(controllers[0]!=null)controllers[0].close();if(stage[0]!=null)stage[0].close();});Platform.exit();}
    }
}
