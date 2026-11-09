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
    @Test void offlineUnlockSaveAndLockUseRealEncryptedSqlite()throws Exception{
        System.setProperty("glass.platform","Monocle");System.setProperty("monocle.platform","Headless");System.setProperty("prism.order","sw");
        var started=new CountDownLatch(1);Platform.startup(started::countDown);assertTrue(started.await(10,TimeUnit.SECONDS));Platform.setImplicitExit(false);
        Path file=temp.resolve("ui.sqlite");var json=new ObjectMapper();String parish="731c7ab4-2fae-4d78-a17b-0ff7653f4501";
        try(var store=LocalStore.unlock(file,"http://localhost:8081/","alice","Local password 1234!".toCharArray(),"alice-id")){
            store.put("session",json.readTree("{\"user\":{\"id\":\"alice-id\",\"username\":\"alice\",\"permissions\":[\"demand:edit\",\"payment:manage\"]}}"));
            store.put("parishes",json.readTree("[{\"id\":\""+parish+"\",\"nom\":\"Saint Paul\"}]"));
            store.put("catalogue:"+parish,json.readTree("{\"types\":[{\"publicId\":\"type-id\",\"libelle\":\"Normale\"}],\"forfaits\":{\"type-id\":[{\"publicId\":\"plan-id\",\"nomForfait\":\"Un jour\",\"nombreCelebration\":1,\"montantForfait\":2000}]},\"horaires\":[{\"publicId\":\"schedule-id\",\"libelle\":\"Matin\"}],\"paiements\":[{\"publicId\":\"cash-id\",\"libelle\":\"Espèces\"}]}"));
        }
        var views=new WorkbenchView[1];var controllers=new WorkbenchController[1];var stage=new javafx.stage.Stage[1];
        try{
            fx(()->{views[0]=new WorkbenchView();controllers[0]=new WorkbenchController(views[0],file,null);stage[0]=new javafx.stage.Stage();stage[0].setScene(new javafx.scene.Scene(views[0].root,1200,850));stage[0].show();views[0].username.setText("alice");views[0].localPassword.setText("Local password 1234!");views[0].offline.fire();});
            await(()->!views[0].workspace.isDisabled() && views[0].plan.getValue()!=null);
            fx(()->{views[0].phone.setText("+22890123456");views[0].intention.setText("Intention enregistrée hors ligne");views[0].save.fire();});
            await(()->views[0].outbox.getItems().size()==1);
            fx(()->{
                var image=views[0].root.snapshot(null,null);
                var buffered=new java.awt.image.BufferedImage((int)image.getWidth(),(int)image.getHeight(),java.awt.image.BufferedImage.TYPE_INT_ARGB);
                for(int y=0;y<buffered.getHeight();y++)for(int x=0;x<buffered.getWidth();x++)buffered.setRGB(x,y,image.getPixelReader().getArgb(x,y));
                try{javax.imageio.ImageIO.write(buffered,"png",Path.of("target","desktop-preview.png").toFile());}catch(java.io.IOException e){throw new RuntimeException(e);}
            });
            try(var reopened=LocalStore.unlock(file,"http://localhost:8081/","alice","Local password 1234!".toCharArray(),null)){assertEquals("DRAFT",reopened.entries().get(0).path("state").asText());}
            fx(()->views[0].lock.fire());await(()->views[0].workspace.isDisabled() && views[0].session.getText().equals("Coffre verrouillé"));
            fx(()->{assertTrue(views[0].outbox.getItems().isEmpty());assertTrue(views[0].intention.getText().isEmpty());});
        }finally{fx(()->{if(controllers[0]!=null)controllers[0].close();if(stage[0]!=null)stage[0].close();});Platform.exit();}
    }
}
