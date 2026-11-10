package com.missanye.desktop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.missanye.desktop.controller.WorkbenchController;
import com.missanye.desktop.repository.LocalStore;
import com.missanye.desktop.view.WorkbenchView;
import javafx.application.Platform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import static org.junit.jupiter.api.Assertions.*;

class MainControllerTest {
    @TempDir Path temp;
    @BeforeAll static void startToolkit()throws Exception{
        System.setProperty("glass.platform","Monocle");System.setProperty("monocle.platform","Headless");System.setProperty("prism.order","sw");System.setProperty("quantum.multithreaded","false");System.setProperty("prism.dirtyopts","false");
        System.setProperty("headless.geometry","2200x1400-32");
        var started=new CountDownLatch(1);Platform.startup(started::countDown);assertTrue(started.await(10,TimeUnit.SECONDS));Platform.setImplicitExit(false);
    }
    @AfterAll static void stopToolkit(){Platform.exit();}
    private static void fx(Runnable action)throws Exception{var task=new FutureTask<Void>(action,null);Platform.runLater(task);task.get(10,TimeUnit.SECONDS);}
    private static void await(BooleanSupplier condition)throws Exception{
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);var result=new java.util.concurrent.atomic.AtomicBoolean();
        while(!result.get() && System.nanoTime()<deadline){fx(()->result.set(condition.getAsBoolean()));if(!result.get())Thread.sleep(20);}assertTrue(result.get());
    }
    private static void capture(WorkbenchView view,String name){
        view.root.applyCss();view.root.layout();
        captureNode(view.root,name);
    }
    private static void captureNode(javafx.scene.Parent node,String name){
        node.applyCss();node.layout();var image=node.snapshot(null,null);
        var buffered=new java.awt.image.BufferedImage((int)image.getWidth(),(int)image.getHeight(),java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<buffered.getHeight();y++)for(int x=0;x<buffered.getWidth();x++)buffered.setRGB(x,y,image.getPixelReader().getArgb(x,y));
        try{javax.imageio.ImageIO.write(buffered,"png",Path.of("target",name).toFile());}catch(java.io.IOException e){throw new RuntimeException(e);}
    }
    @Test void offlineUnlockSaveAndLockUseRealEncryptedSqlite()throws Exception{
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
            fx(()->{views[0]=new WorkbenchView();controllers[0]=new WorkbenchController(views[0],file,null,"http://localhost:8081");stage[0]=new javafx.stage.Stage();stage[0].setScene(new javafx.scene.Scene(views[0].root,1200,850));stage[0].show();assertNull(views[0].root.lookup("#server"));capture(views[0],"desktop-login-preview.png");views[0].username.setText("alice");views[0].localPassword.setText("Local password 1234!");});
            await(()->!views[0].offline.isDisabled());fx(()->views[0].offline.fire());
            await(()->!views[0].workspace.isDisabled() && views[0].plan.getValue()!=null);
            fx(()->{
                capture(views[0],"desktop-dashboard-preview.png");assertEquals(3,views[0].tabs.getSelectionModel().getSelectedIndex());
                assertEquals(3,views[0].demands.getItems().size());assertEquals("3",views[0].demandCount.getText());
                @SuppressWarnings("unchecked") var recent=(javafx.scene.control.ListView<com.missanye.desktop.model.WorkspaceModels.Row>)views[0].root.lookup("#recentDemands");assertEquals(3,recent.getItems().size());
                views[0].demandSearch.setText("aucun résultat");recent.getSelectionModel().selectFirst();recent.fireEvent(new javafx.scene.input.KeyEvent(javafx.scene.input.KeyEvent.KEY_PRESSED,"","",javafx.scene.input.KeyCode.ENTER,false,false,false,false));
                assertEquals(1,views[0].tabs.getSelectionModel().getSelectedIndex());assertTrue(views[0].demandSearch.getText().isEmpty());assertEquals(recent.getSelectionModel().getSelectedItem(),views[0].demands.getSelectionModel().getSelectedItem());
                var amountColumn=views[0].demands.getColumns().get(3);amountColumn.setSortType(javafx.scene.control.TableColumn.SortType.DESCENDING);views[0].demands.getSortOrder().setAll(amountColumn);views[0].demands.sort();assertEquals(20000,views[0].demands.getItems().get(0).data().path("montant").asInt());views[0].demands.getSortOrder().clear();
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
        }finally{fx(()->{if(controllers[0]!=null)controllers[0].close();if(stage[0]!=null)stage[0].close();});}
    }
    private static void answerNextDialog(javafx.scene.control.ButtonBar.ButtonData decision){
        Platform.runLater(()->{
            for(var window:java.util.List.copyOf(javafx.stage.Window.getWindows())){
                if(window.getScene()==null)continue;
                var root=window.getScene().getRoot();var node=root instanceof javafx.scene.control.DialogPane?root:root.lookup(".dialog-pane");
                if(node instanceof javafx.scene.control.DialogPane pane)for(var type:pane.getButtonTypes())if(type.getButtonData()==decision){((javafx.scene.control.Button)pane.lookupButton(type)).fire();return;}
            }
            fail("Le dialogue attendu n'est pas affiché");
        });
    }
    private static com.fasterxml.jackson.databind.JsonNode identity()throws Exception{
        return new ObjectMapper().readTree("{\"user\":{\"id\":\"alice-id\",\"username\":\"alice\",\"permissions\":[\"demand:edit\",\"payment:manage\"]},\"paroisses\":[{\"id\":\""+TestRequests.PARISH+"\",\"nom\":\"Saint Paul\"},{\"id\":\"731c7ab4-2fae-4d78-a17b-0ff7653f4599\",\"nom\":\"Saint Pierre\"}]}");
    }
    private static com.fasterxml.jackson.databind.node.ObjectNode catalogue()throws Exception{
        var p=TestRequests.complete();var json=new ObjectMapper();var c=json.createObjectNode();
        c.putArray("types").addObject().put("publicId",p.path("typeDemandePublicId").asText()).put("libelle","Normale");
        c.putObject("forfaits").putArray(p.path("typeDemandePublicId").asText()).addObject().put("publicId",p.path("forfaitTarifPublicId").asText()).put("nomForfait","Un jour").put("natureForfait","NORMALE").put("nombreCelebration",1).put("montantForfait",2000);
        c.putArray("horaires").addObject().put("publicId",p.path("horairePublicId").asText()).put("libelle","Matin");
        c.putArray("paiements").addObject().put("publicId",p.path("typePaiementPublicId").asText()).put("libelle","Espèces");return c;
    }
    private static void prepare(Path file,String address)throws Exception{
        try(var store=LocalStore.unlock(file,address,"alice","Local password 1234!".toCharArray(),"alice-id")){
            store.put("session",identity());store.put("parishes",identity().path("paroisses"));store.put("catalogue:"+TestRequests.PARISH,catalogue());store.put("demands:"+TestRequests.PARISH,new ObjectMapper().createArrayNode());
            store.put("catalogue:731c7ab4-2fae-4d78-a17b-0ff7653f4599",catalogue());store.put("demands:731c7ab4-2fae-4d78-a17b-0ff7653f4599",new ObjectMapper().createArrayNode());
        }
    }
    @Test void cancelProtectsSaisieAndSaveBeforeLogoutKeepsIncompleteDraft()throws Exception{
        Path file=temp.resolve("guards.sqlite");prepare(file,"http://localhost:8081/");var views=new WorkbenchView[1];var controllers=new WorkbenchController[1];var stage=new javafx.stage.Stage[1];
        try{
            fx(()->{views[0]=new WorkbenchView();controllers[0]=new WorkbenchController(views[0],file,null,"http://localhost:8081");stage[0]=new javafx.stage.Stage();stage[0].setScene(new javafx.scene.Scene(views[0].root,1100,800));stage[0].show();views[0].username.setText("alice");views[0].localPassword.setText("Local password 1234!");});
            await(()->!views[0].offline.isDisabled());fx(()->views[0].offline.fire());
            await(()->!views[0].workspace.isDisabled()&&views[0].plan.getValue()!=null);
            fx(()->{
                views[0].intention.setText("Brouillon partiel à conserver");views[0].date.getEditor().setText("date à compléter");assertTrue(controllers[0].isDirty());
                answerNextDialog(javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);views[0].fresh.fire();assertEquals("Brouillon partiel à conserver",views[0].intention.getText());
                var closed=new java.util.concurrent.atomic.AtomicBoolean();answerNextDialog(javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);controllers[0].requestClose(()->closed.set(true));assertFalse(closed.get());
                answerNextDialog(javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);views[0].parish.getSelectionModel().select(1);assertEquals(TestRequests.PARISH,views[0].parish.getValue().id());assertEquals("Brouillon partiel à conserver",views[0].intention.getText());
                answerNextDialog(javafx.scene.control.ButtonBar.ButtonData.OK_DONE);views[0].lock.fire();
            });
            await(()->views[0].workspace.isDisabled()&&views[0].session.getText().equals("Coffre verrouillé"));
            try(var store=LocalStore.unlock(file,"http://localhost:8081/","alice","Local password 1234!".toCharArray(),null)){
                assertEquals(1,store.entries().size());var draft=store.entries().get(0);assertEquals("Brouillon partiel à conserver",draft.path("payload").path("intention").asText());assertEquals("",draft.path("payload").path("telFidele").asText());assertEquals("date à compléter",draft.path("payload").path("_invalidDateText").asText());assertEquals("DRAFT",draft.path("state").asText());assertThrows(IllegalArgumentException.class,()->store.queue(draft.path("id").asText()));
            }
        }finally{fx(()->{if(controllers[0]!=null)controllers[0].close();if(stage[0]!=null)stage[0].close();});}
    }
    private static void reply(com.sun.net.httpserver.HttpExchange e,int code,String body)throws java.io.IOException{byte[] bytes=body.getBytes(java.nio.charset.StandardCharsets.UTF_8);e.sendResponseHeaders(code,bytes.length);e.getResponseBody().write(bytes);e.close();}
    @Test void thirtyCelebrationDialogFitsScreenAndInvalidTimeKeepsItOpen()throws Exception{
        Path file=temp.resolve("slots.sqlite");prepare(file,"http://localhost:8081/");var views=new WorkbenchView[1];var controllers=new WorkbenchController[1];var stage=new javafx.stage.Stage[1];
        try{
            fx(()->{views[0]=new WorkbenchView();controllers[0]=new WorkbenchController(views[0],file,null,"http://localhost:8081/");stage[0]=new javafx.stage.Stage();stage[0].setScene(new javafx.scene.Scene(views[0].root,1024,700));stage[0].show();views[0].username.setText("alice");views[0].localPassword.setText("Local password 1234!");});
            await(()->!views[0].offline.isDisabled());fx(()->views[0].offline.fire());await(()->!views[0].workspace.isDisabled()&&views[0].plan.getValue()!=null);
            fx(()->{
                for(int day=1;day<30;day++)views[0].extraDates.getItems().add(views[0].date.getValue().plusDays(day));
                Platform.runLater(()->{
                    for(var window:java.util.List.copyOf(javafx.stage.Window.getWindows())){
                        if(window.getScene()==null)continue;var root=window.getScene().getRoot();var pane=root instanceof javafx.scene.control.DialogPane d?d:(javafx.scene.control.DialogPane)root.lookup(".dialog-pane");
                        if(pane==null||pane.lookup("#slotTime0")==null)continue;
                        var field=(javafx.scene.control.TextField)pane.lookup("#slotTime0");var apply=(javafx.scene.control.Button)pane.lookupButton(pane.getButtonTypes().get(1));
                        assertNotNull(pane.lookup("#slotTime29"));assertTrue(window.getHeight()<=javafx.stage.Screen.getPrimary().getVisualBounds().getHeight()-40);
                        field.setText("25:99");apply.fire();assertTrue(window.isShowing());assertFalse(((javafx.scene.control.Label)pane.lookup("#slotValidation")).getText().isBlank());
                        captureNode(pane,"desktop-slots-preview.png");field.setText("06:30");apply.fire();return;
                    }fail("Dialogue des créneaux absent");
                });views[0].configureSlots.fire();assertTrue(views[0].slotsSummary.getText().startsWith("30"));
            });
        }finally{fx(()->{if(controllers[0]!=null)controllers[0].close();if(stage[0]!=null)stage[0].close();});}
    }
    @Test void navigationAndPagesAdaptToWindowsSizesWithoutLosingDraft()throws Exception{
        Path file=temp.resolve("responsive.sqlite");prepare(file,"http://localhost:8081/");var views=new WorkbenchView[1];var controllers=new WorkbenchController[1];var stage=new javafx.stage.Stage[1];
        try{
            fx(()->{views[0]=new WorkbenchView();controllers[0]=new WorkbenchController(views[0],file,null,"http://localhost:8081/");stage[0]=new javafx.stage.Stage();stage[0].setScene(new javafx.scene.Scene(views[0].root,1366,768));stage[0].show();views[0].username.setText("alice");views[0].localPassword.setText("Local password 1234!");});
            await(()->!views[0].offline.isDisabled());fx(()->views[0].offline.fire());await(()->!views[0].workspace.isDisabled()&&views[0].plan.getValue()!=null);
            fx(()->views[0].intention.setText("Saisie conservée pendant la navigation et le redimensionnement"));
            for(int[] size:new int[][]{{900,600},{1024,768},{1366,768},{1600,1000},{1920,1080}}){
                fx(()->{stage[0].setWidth(size[0]);stage[0].setHeight(size[1]);});await(()->Math.abs(views[0].root.getWidth()-size[0])<30);
                fx(()->{
                    var v=views[0];((javafx.scene.control.Button)v.root.lookup("#navHome")).fire();v.root.applyCss();v.root.layout();
                    assertEquals(3,v.tabs.getSelectionModel().getSelectedIndex());assertEquals("Tableau de bord",((javafx.scene.control.Label)v.root.lookup("#pageTitle")).getText());
                    var nav=(javafx.scene.control.Button)v.root.lookup("#navHome");assertTrue(nav.getStyleClass().contains("nav-active"));assertEquals(size[0]<1180?"":"Tableau de bord",nav.getText());
                    capture(v,"desktop-dashboard-"+size[0]+"x"+size[1]+".png");
                    ((javafx.scene.control.Button)v.root.lookup("#navRequest")).fire();assertEquals(0,v.tabs.getSelectionModel().getSelectedIndex());assertTrue(v.intention.getText().startsWith("Saisie conservée"));
                    ((javafx.scene.control.Button)v.root.lookup("#navDemands")).fire();v.root.layout();assertEquals(1,v.tabs.getSelectionModel().getSelectedIndex());assertTrue(v.demands.getWidth()>300);
                    capture(v,"desktop-demands-"+size[0]+"x"+size[1]+".png");
                    ((javafx.scene.control.Button)v.root.lookup("#navActivity")).fire();v.root.layout();((javafx.scene.control.ScrollPane)v.root.getCenter()).setVvalue(1);v.root.layout();
                    assertTrue(v.queue.localToScene(v.queue.getBoundsInLocal()).getMaxY()<=v.root.getHeight());
                });
            }
        }finally{fx(()->{if(controllers[0]!=null)controllers[0].close();if(stage[0]!=null)stage[0].close();});}
    }
    private static javafx.scene.control.DialogPane deploymentDialog(){
        for(var window:java.util.List.copyOf(javafx.stage.Window.getWindows())){if(window.getScene()==null)continue;var root=window.getScene().getRoot();var node=root instanceof javafx.scene.control.DialogPane?root:root.lookup(".dialog-pane");if(node instanceof javafx.scene.control.DialogPane pane&&pane.lookup("#deploymentServer")!=null)return pane;}return null;
    }
    @Test void developerBindsParishThenUsersSignInNormallyWithRbac()throws Exception{
        var server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);var info=identity();var c=catalogue();String type=TestRequests.complete().path("typeDemandePublicId").asText();var developer=new java.util.concurrent.atomic.AtomicBoolean(true);
        ((com.fasterxml.jackson.databind.node.ObjectNode)info.path("user")).put("role","SECRETAIRE").put("isGlobal",false);
        var ownParish=info.path("paroisses").get(0).deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)info).putArray("paroisses").add(ownParish);
        server.createContext("/actuator/health",e->reply(e,200,"{\"status\":\"UP\"}"));server.createContext("/auth/login-multi-tenant",e->reply(e,200,"{}"));server.createContext("/auth/me",e->reply(e,200,info.toString()));server.createContext("/auth/logout",e->reply(e,200,"{}"));server.createContext("/paroisses",e->reply(e,200,"[{\"publicId\":\""+TestRequests.PARISH+"\",\"nom\":\"Saint Paul\",\"isActive\":true}]"));
        server.createContext("/type-demandes/",e->reply(e,200,c.path("types").toString()));server.createContext("/horaires/",e->reply(e,200,c.path("horaires").toString()));server.createContext("/type-paiement",e->reply(e,200,c.path("paiements").toString()));server.createContext("/forfait-tarifs/",e->reply(e,200,c.path("forfaits").path(type).toString()));server.createContext("/demandes/",e->reply(e,200,"{\"content\":[],\"last\":true}"));server.start();
        String address="http://127.0.0.1:"+server.getAddress().getPort()+"/";Path file=temp.resolve("bound.sqlite");var views=new WorkbenchView[1];var controllers=new WorkbenchController[1];var stage=new javafx.stage.Stage[1];
        try{
            fx(()->{views[0]=new WorkbenchView();controllers[0]=new WorkbenchController(views[0],file,null);stage[0]=new javafx.stage.Stage();stage[0].setScene(new javafx.scene.Scene(views[0].root,1366,768));stage[0].show();assertTrue(views[0].login.isDisabled());Platform.runLater(views[0].configureServer::fire);});await(()->deploymentDialog()!=null);
            fx(()->{var pane=deploymentDialog();((javafx.scene.control.TextField)pane.lookup("#deploymentServer")).setText(address);((javafx.scene.control.TextField)pane.lookup("#deploymentUser")).setText("admin");((javafx.scene.control.PasswordField)pane.lookup("#deploymentPassword")).setText("server admin password");((javafx.scene.control.Button)pane.lookup("#deploymentVerify")).fire();});
            await(()->((javafx.scene.control.ComboBox<?>)deploymentDialog().lookup("#deploymentParish")).getValue()!=null);
            fx(()->{var pane=deploymentDialog();captureNode(pane,"desktop-parish-deployment-preview.png");((javafx.scene.control.Button)pane.lookupButton(pane.getButtonTypes().get(1))).fire();});await(()->deploymentDialog()==null);
            var binding=com.missanye.desktop.config.ServerSettings.deployment(file).orElseThrow();assertEquals(TestRequests.PARISH,binding.parishId());
            ((com.fasterxml.jackson.databind.node.ObjectNode)info.path("user")).put("role","LECTEUR").put("isGlobal",false).putArray("permissions").add("demand:read").add("payment:read").add("invoice:read");
            fx(()->{controllers[0].close();controllers[0]=new WorkbenchController(views[0],file,null,binding,new com.missanye.desktop.service.DeviceCredentials(temp.resolve("device"),DeviceCredentialsTest.testProtector()));assertFalse(views[0].offlinePane.isVisible());capture(views[0],"desktop-parish-login-preview.png");views[0].username.setText("alice");views[0].password.setText("normal password");views[0].login.fire();});
            await(()->!views[0].workspace.isDisabled()&&views[0].plan.getValue()!=null);
            fx(()->{assertTrue(views[0].parish.isDisabled());assertEquals(1,views[0].parish.getItems().size());assertEquals("Saint Paul",views[0].parish.getValue().label());assertFalse(views[0].save.isVisible(),"save");assertFalse(views[0].cash.isVisible(),"cash");assertFalse(views[0].edit.isVisible(),"draft edit");assertFalse(views[0].queue.isVisible(),"queue");assertFalse(views[0].sync.isVisible(),"sync");assertFalse(views[0].actionVisible("navRequest"),"nav");assertTrue(views[0].receipt.isVisible(),"receipt");assertFalse(views[0].hasOpenDialog(),"dialog");capture(views[0],"desktop-parish-reader-preview.png");});
        }finally{fx(()->{if(controllers[0]!=null)controllers[0].close();if(stage[0]!=null)stage[0].close();});server.stop(0);}
    }
    @Test void offlineParishFinalizesReceivesCashThenReconnectsWithoutLosingSaisie()throws Exception{
        var server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);var status=new java.util.concurrent.atomic.AtomicInteger(200);var created=new java.util.concurrent.atomic.AtomicReference<com.fasterxml.jackson.databind.JsonNode>();var calls=new java.util.concurrent.atomic.AtomicInteger();var gate=new java.util.concurrent.atomic.AtomicReference<java.util.concurrent.CountDownLatch>();var reconnectStarted=new java.util.concurrent.CountDownLatch(1);var operationGate=new java.util.concurrent.CountDownLatch(1);var operationStarted=new java.util.concurrent.CountDownLatch(1);
        var info=identity();((com.fasterxml.jackson.databind.node.ObjectNode)info.path("user")).put("role","ADMIN").putArray("permissions").add("demand:read").add("demand:edit").add("demand:validate").add("payment:manage").add("invoice:read");var c=catalogue();String type=TestRequests.complete().path("typeDemandePublicId").asText();
        server.createContext("/auth/login-multi-tenant",e->{var latch=gate.get();if(status.get()==200&&latch!=null){reconnectStarted.countDown();try{latch.await(5,java.util.concurrent.TimeUnit.SECONDS);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();}}reply(e,status.get(),"{}");});server.createContext("/auth/me",e->reply(e,200,info.toString()));
        server.createContext("/type-demandes/",e->reply(e,200,c.path("types").toString()));server.createContext("/horaires/",e->reply(e,200,c.path("horaires").toString()));server.createContext("/type-paiement",e->reply(e,200,c.path("paiements").toString()));server.createContext("/forfait-tarifs/",e->reply(e,200,c.path("forfaits").path(type).toString()));
        server.createContext("/demandes/",e->reply(e,200,"{\"content\":"+(created.get()==null?"[]":"["+created.get()+"]")+",\"last\":true}"));
        server.createContext("/desktop/operations",e->{try{var command=new ObjectMapper().readTree(e.getRequestBody());if("CREATE_DEMANDE".equals(command.path("kind").asText())){operationStarted.countDown();operationGate.await(5,java.util.concurrent.TimeUnit.SECONDS);}calls.incrementAndGet();var result=command.path("payload").deepCopy();if("CREATE_DEMANDE".equals(command.path("kind").asText())){((com.fasterxml.jackson.databind.node.ObjectNode)result).put("localReference",com.missanye.desktop.service.LocalRequestService.reference(command.path("operationId").asText())).put("publicId","a31c7ab4-2fae-4d78-a17b-0ff7653f4501").put("codeSuivie","MS-SP-ABCDEFGHJK").put("montant",2000).put("statutPaiement","NON_PAYE").put("statutValidation","VALIDEE").put("updatedAt","2026-11-10T10:00:00");created.set(result);}else{result=created.get().deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)result).put("statutPaiement","PAYE");created.set(result);}reply(e,200,result.toString());}catch(Exception failure){e.close();}});server.start();
        String address="http://127.0.0.1:"+server.getAddress().getPort()+"/";Path file=temp.resolve("resume.sqlite");var binding=new com.missanye.desktop.config.ParishDeployment(address,TestRequests.PARISH,"Saint Paul");var credentials=new com.missanye.desktop.service.DeviceCredentials(temp.resolve("resume-identity"),DeviceCredentialsTest.testProtector());
        var prepared=com.missanye.desktop.service.ParishLoginService.login(file,binding,credentials,"alice","normal password".toCharArray());
        var oldRows=new ObjectMapper().createArrayNode();oldRows.addObject().put("publicId","b31c7ab4-2fae-4d78-a17b-0ff7653f4501").put("paroissePublicId",TestRequests.PARISH).put("codeSuivie","MS-OLD-ABCDEFGHJK").put("montant",4000).put("statutPaiement","NON_PAYE").put("statutValidation","VALIDEE");prepared.store().put("demands:"+TestRequests.PARISH,oldRows);prepared.store().close();prepared.api().clear();status.set(503);
        var views=new WorkbenchView[1];var controllers=new WorkbenchController[1];var stage=new javafx.stage.Stage[1];
        try{
            fx(()->{views[0]=new WorkbenchView();controllers[0]=new WorkbenchController(views[0],file,null,binding,credentials);stage[0]=new javafx.stage.Stage();stage[0].setScene(new javafx.scene.Scene(views[0].root,1366,768));stage[0].show();views[0].username.setText("alice");views[0].password.setText("normal password");views[0].login.fire();});await(()->!views[0].workspace.isDisabled()&&views[0].plan.getValue()!=null);
            fx(()->{var amount=views[0].demands.getColumns().get(3);amount.setSortType(javafx.scene.control.TableColumn.SortType.ASCENDING);views[0].demands.getSortOrder().setAll(amount);views[0].tabs.getSelectionModel().select(0);views[0].phone.setText("+22890123456");views[0].intention.setText("Intention enregistrée pendant la coupure");answerNextDialog(javafx.scene.control.ButtonBar.ButtonData.OK_DONE);views[0].finalizeRequest.fire();});await(()->!views[0].workspace.isDisabled()&&views[0].demands.getItems().size()==2);
            fx(()->{assertTrue(views[0].demands.getSelectionModel().getSelectedItem().data().path("codeSuivie").asText().startsWith("MSL-"));assertFalse(views[0].cash.isDisabled());answerNextDialog(javafx.scene.control.ButtonBar.ButtonData.OK_DONE);views[0].cash.fire();});await(()->!views[0].workspace.isDisabled()&&views[0].activityRows.size()==2);
            status.set(200);gate.set(new java.util.concurrent.CountDownLatch(1));fx(()->{views[0].intention.setText("Saisie à conserver pendant la reconnexion");assertFalse(views[0].sync.isDisabled());views[0].sync.fire();});assertTrue(reconnectStarted.await(3,java.util.concurrent.TimeUnit.SECONDS));
            fx(()->{assertFalse(views[0].workspace.isDisabled());assertFalse(views[0].intention.isDisabled());});gate.get().countDown();assertTrue(operationStarted.await(3,java.util.concurrent.TimeUnit.SECONDS));fx(()->{assertFalse(views[0].workspace.isDisabled());assertFalse(views[0].intention.isDisabled());});operationGate.countDown();await(()->calls.get()==2&&views[0].activityRows.stream().allMatch(r->"SENT".equals(r.data().path("state").asText())));
            fx(()->{assertEquals("Saisie à conserver pendant la reconnexion",views[0].intention.getText());assertTrue(views[0].activityRows.stream().allMatch(r->"SENT".equals(r.data().path("state").asText())));assertEquals("PAYE",views[0].demandRows.get(0).data().path("statutPaiement").asText());assertFalse(views[0].hasOpenDialog());capture(views[0],"desktop-local-synchronized-preview.png");});
        }finally{fx(()->{if(controllers[0]!=null)controllers[0].close();if(stage[0]!=null)stage[0].close();});server.stop(0);}
    }
    private static javafx.scene.control.DialogPane serverDialog(){
        for(var window:java.util.List.copyOf(javafx.stage.Window.getWindows())){
            if(window.getScene()==null)continue;var root=window.getScene().getRoot();var node=root instanceof javafx.scene.control.DialogPane?root:root.lookup(".dialog-pane");
            if(node instanceof javafx.scene.control.DialogPane pane&&pane.lookup("#serverAddress")!=null)return pane;
        }return null;
    }
    private static javafx.scene.control.Button saveServerButton(){var pane=serverDialog();return (javafx.scene.control.Button)pane.lookupButton(pane.getButtonTypes().stream().filter(b->b.getButtonData()==javafx.scene.control.ButtonBar.ButtonData.OK_DONE).findFirst().orElseThrow());}
    @Test void serverConfigurationRequiresSuccessfulTestAndChangesLoginDestination()throws Exception{
        var server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);var logins=new java.util.concurrent.atomic.AtomicInteger();
        server.createContext("/actuator/health",e->reply(e,200,"{\"status\":\"UP\"}"));server.createContext("/auth/login-multi-tenant",e->{logins.incrementAndGet();reply(e,401,"{}");});server.start();
        String address="http://127.0.0.1:"+server.getAddress().getPort()+"/";Path file=temp.resolve("configuration.sqlite");var views=new WorkbenchView[1];var controllers=new WorkbenchController[1];var stage=new javafx.stage.Stage[1];
        try{
            fx(()->{views[0]=new WorkbenchView();controllers[0]=new WorkbenchController(views[0],file,null,"http://localhost:8081/");stage[0]=new javafx.stage.Stage();stage[0].setScene(new javafx.scene.Scene(views[0].root,1100,850));stage[0].show();Platform.runLater(views[0].configureServer::fire);});
            await(()->serverDialog()!=null);
            fx(()->{var pane=serverDialog();assertTrue(saveServerButton().isDisabled());((javafx.scene.control.TextField)pane.lookup("#serverAddress")).setText("http://invalid.example");((javafx.scene.control.Button)pane.lookup("#testServer")).fire();assertTrue(saveServerButton().isDisabled());((javafx.scene.control.TextField)pane.lookup("#serverAddress")).setText(address);((javafx.scene.control.Button)pane.lookup("#testServer")).fire();});
            await(()->!saveServerButton().isDisabled());
            fx(()->{var pane=serverDialog();assertTrue(((javafx.scene.control.Label)pane.lookup("#serverResultTitle")).getText().contains("réussie"));captureNode(pane,"desktop-server-settings-preview.png");((javafx.scene.control.TextField)pane.lookup("#serverAddress")).setText(address+"changed/");assertTrue(saveServerButton().isDisabled());((javafx.scene.control.Button)pane.lookup("#testServer")).fire();});
            await(()->((javafx.scene.control.Label)serverDialog().lookup("#serverResultTitle")).getText().contains("Échec"));
            fx(()->{var pane=serverDialog();assertTrue(saveServerButton().isDisabled());assertTrue(((javafx.scene.control.Label)pane.lookup("#serverResult")).getText().contains("HTTP 404"));assertTrue(pane.getScene().getWindow() instanceof javafx.stage.Stage stageWindow&&stageWindow.isResizable());pane.getScene().getWindow().setWidth(540);pane.getScene().getWindow().setHeight(420);pane.applyCss();pane.layout();var save=saveServerButton();assertTrue(save.localToScene(save.getBoundsInLocal()).getMaxY()<=pane.getScene().getHeight());captureNode(pane,"desktop-server-failure-preview.png");((javafx.scene.control.TextField)pane.lookup("#serverAddress")).setText(address);((javafx.scene.control.Button)pane.lookup("#testServer")).fire();});
            await(()->!saveServerButton().isDisabled());fx(()->saveServerButton().fire());await(()->serverDialog()==null);
            assertEquals(address,com.missanye.desktop.config.ServerSettings.load(file,"https://example.com/"));
            fx(()->{views[0].username.setText("alice");views[0].password.setText("not persisted");views[0].login.fire();});
            await(()->!views[0].login.isDisabled()&&logins.get()==1);assertFalse(java.nio.file.Files.exists(file));
            fx(()->{controllers[0].close();controllers[0]=new WorkbenchController(views[0],file,null);assertTrue(views[0].login.isDisabled());assertTrue(views[0].serverEnvironment.getText().contains("rattaché"));});
        }finally{fx(()->{if(controllers[0]!=null)controllers[0].close();if(stage[0]!=null)stage[0].close();});server.stop(0);}
    }
    private static javafx.scene.control.DialogPane setupDialog(){
        for(var window:java.util.List.copyOf(javafx.stage.Window.getWindows())){
            if(window.getScene()==null)continue;
            var root=window.getScene().getRoot();var node=root instanceof javafx.scene.control.DialogPane?root:root.lookup(".dialog-pane");
            if(node instanceof javafx.scene.control.DialogPane pane&&pane.lookup("#setupPassword")!=null)return pane;
        }
        return null;
    }
    @Test void firstConnectionConfirmsProtectionAndEnablesOfflineOnlyAfterPreparation()throws Exception{firstConnection(true);}
    @Test void interruptedFirstPreparationKeepsOfflineLockedAndCreatesNoVault()throws Exception{firstConnection(false);}
    private void firstConnection(boolean success)throws Exception{
        var server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
        var info=identity();var c=catalogue();String type=TestRequests.complete().path("typeDemandePublicId").asText();
        server.createContext("/auth/login-multi-tenant",e->reply(e,200,info.toString()));server.createContext("/auth/me",e->reply(e,200,info.toString()));server.createContext("/auth/logout",e->reply(e,200,"{}"));
        server.createContext("/type-demandes/",e->reply(e,200,c.path("types").toString()));server.createContext("/horaires/",e->reply(e,200,c.path("horaires").toString()));server.createContext("/type-paiement",e->reply(e,200,c.path("paiements").toString()));server.createContext("/forfait-tarifs/",e->reply(e,200,c.path("forfaits").path(type).toString()));server.createContext("/demandes/paroisse/",e->reply(e,success?200:503,success?"{\"content\":[],\"last\":true}":"{\"message\":\"Service temporairement indisponible\"}"));server.start();
        String address="http://127.0.0.1:"+server.getAddress().getPort()+"/";Path file=temp.resolve("first.sqlite");var views=new WorkbenchView[1];var controllers=new WorkbenchController[1];var stage=new javafx.stage.Stage[1];
        try{
            fx(()->{views[0]=new WorkbenchView();controllers[0]=new WorkbenchController(views[0],file,null,address);stage[0]=new javafx.stage.Stage();stage[0].setScene(new javafx.scene.Scene(views[0].root,1100,800));stage[0].show();views[0].username.setText("alice");assertTrue(views[0].offline.isDisabled());views[0].password.setText("server password");views[0].login.fire();});
            await(()->setupDialog()!=null);
            assertFalse(java.nio.file.Files.exists(file));
            fx(()->{
                var pane=setupDialog();var button=(javafx.scene.control.Button)pane.lookupButton(pane.getButtonTypes().stream().filter(b->b.getButtonData()==javafx.scene.control.ButtonBar.ButtonData.OK_DONE).findFirst().orElseThrow());
                captureNode(pane,"desktop-first-connection-preview.png");assertTrue(button.isDisabled());
                ((javafx.scene.control.ComboBox<?>)pane.lookup("#setupParish")).getSelectionModel().select(1);
                ((javafx.scene.control.PasswordField)pane.lookup("#setupPassword")).setText("Local password 1234!");
                ((javafx.scene.control.PasswordField)pane.lookup("#setupConfirmation")).setText("Different password");assertTrue(button.isDisabled());
                ((javafx.scene.control.PasswordField)pane.lookup("#setupConfirmation")).setText("Local password 1234!");assertFalse(button.isDisabled());button.fire();
            });
            if(!success){
                await(()->setupDialog()==null&&!views[0].login.isDisabled()&&views[0].status.getText().contains("indisponible"));
                fx(()->{assertTrue(views[0].workspace.isDisabled());assertTrue(views[0].offline.isDisabled());});
                assertFalse(java.nio.file.Files.exists(file));return;
            }
            await(()->!views[0].workspace.isDisabled()&&views[0].plan.getValue()!=null);
            assertEquals(LocalStore.ProfileStatus.READY,LocalStore.inspect(file,address,"alice"));
            fx(()->{assertEquals("Saint Pierre",views[0].parish.getValue().label());views[0].lock.fire();});
            await(()->views[0].workspace.isDisabled()&&!views[0].offline.isDisabled());server.stop(0);
            fx(()->{views[0].localPassword.setText("Local password 1234!");views[0].offline.fire();});
            await(()->!views[0].workspace.isDisabled()&&views[0].plan.getValue()!=null);
            fx(()->{assertEquals("Saint Pierre",views[0].parish.getValue().label());assertTrue(views[0].session.getText().startsWith("Hors ligne"));});
        }finally{fx(()->{if(controllers[0]!=null)controllers[0].close();if(stage[0]!=null)stage[0].close();});server.stop(0);}
    }
    @Test void refreshKeepsEditedFieldsAndSelectedPlan()throws Exception{
        var server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);var loads=new java.util.concurrent.atomic.AtomicInteger();var json=new ObjectMapper();var info=identity();var c=catalogue();String type=TestRequests.complete().path("typeDemandePublicId").asText();String second="731c7ab4-2fae-4d78-a17b-0ff7653f4555";
        ((com.fasterxml.jackson.databind.node.ArrayNode)c.path("forfaits").path(type)).addObject().put("publicId",second).put("nomForfait","Deuxième forfait").put("nombreCelebration",1).put("montantForfait",5000);
        server.createContext("/auth/login-multi-tenant",e->reply(e,200,info.toString()));server.createContext("/auth/me",e->reply(e,200,info.toString()));server.createContext("/auth/logout",e->reply(e,200,"{}"));
        server.createContext("/type-demandes/",e->{var types=c.path("types").deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)types.get(0)).put("libelle","Normale v"+loads.incrementAndGet());reply(e,200,types.toString());});
        server.createContext("/horaires/",e->reply(e,200,c.path("horaires").toString()));server.createContext("/type-paiement",e->reply(e,200,c.path("paiements").toString()));server.createContext("/forfait-tarifs/",e->reply(e,200,c.path("forfaits").path(type).toString()));server.createContext("/demandes/paroisse/",e->{var response=json.createObjectNode().put("last",true);var rows=response.putArray("content");if(loads.get()>1)rows.addObject().put("publicId","another-demand").put("intention","Demande ajoutée ailleurs");rows.addObject().put("publicId","selected-demand").put("intention","Demande sélectionnée");reply(e,200,response.toString());});server.start();
        String address="http://127.0.0.1:"+server.getAddress().getPort()+"/";Path file=temp.resolve("refresh.sqlite");prepare(file,address);var views=new WorkbenchView[1];var controllers=new WorkbenchController[1];var stage=new javafx.stage.Stage[1];
        try{
            fx(()->{views[0]=new WorkbenchView();controllers[0]=new WorkbenchController(views[0],file,null,address);stage[0]=new javafx.stage.Stage();stage[0].setScene(new javafx.scene.Scene(views[0].root,1100,800));stage[0].show();views[0].username.setText("alice");views[0].password.setText("server password");views[0].localPassword.setText("Local password 1234!");views[0].login.fire();});
            await(()->!views[0].workspace.isDisabled()&&views[0].plan.getItems().size()==2);
            fx(()->{views[0].demands.getSelectionModel().selectFirst();views[0].plan.getSelectionModel().select(1);views[0].intention.setText("Cette intention doit survivre à l'actualisation");views[0].refresh.fire();var closed=new java.util.concurrent.atomic.AtomicBoolean();controllers[0].requestClose(()->closed.set(true));assertFalse(closed.get());assertTrue(views[0].status.getText().contains("opération est en cours"));});
            await(()->loads.get()>=2&&!views[0].workspace.isDisabled());
            fx(()->{assertEquals(second,views[0].plan.getValue().id());assertEquals("Cette intention doit survivre à l'actualisation",views[0].intention.getText());assertTrue(controllers[0].isDirty());assertEquals("selected-demand",views[0].demands.getSelectionModel().getSelectedItem().data().path("publicId").asText());});
        }finally{fx(()->{if(controllers[0]!=null)controllers[0].close();if(stage[0]!=null)stage[0].close();});server.stop(0);}
    }

    @Test void globalSeedAccountWithoutParishesGetsSuccessfulAuthenticationNotice()throws Exception{
        var server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);var info=identity();((com.fasterxml.jackson.databind.node.ObjectNode)info.path("user")).put("isGlobal",true);
        server.createContext("/auth/login-multi-tenant",e->reply(e,200,info.toString()));server.createContext("/auth/me",e->reply(e,200,info.toString()));server.createContext("/paroisses",e->reply(e,200,"[]"));server.start();
        String address="http://127.0.0.1:"+server.getAddress().getPort()+"/";Path file=temp.resolve("global-empty.sqlite");var views=new WorkbenchView[1];var controllers=new WorkbenchController[1];var stage=new javafx.stage.Stage[1];
        try{
            fx(()->{views[0]=new WorkbenchView();controllers[0]=new WorkbenchController(views[0],file,null,address);stage[0]=new javafx.stage.Stage();stage[0].setScene(new javafx.scene.Scene(views[0].root,1024,700));stage[0].show();views[0].username.setText("admin");views[0].password.setText("seed password");views[0].login.fire();});
            await(()->views[0].hasOpenDialog()&&views[0].status.getText().contains("Connexion réussie"));
            fx(()->{assertTrue(views[0].workspace.isDisabled());assertTrue(views[0].loginStatus.getText().contains("compte administrateur vérifié"));answerNextDialog(javafx.scene.control.ButtonBar.ButtonData.OK_DONE);});
            await(()->!views[0].hasOpenDialog());assertFalse(java.nio.file.Files.exists(file));
        }finally{fx(()->{if(controllers[0]!=null)controllers[0].close();if(stage[0]!=null)stage[0].close();});server.stop(0);}
    }
    @Test void failedInitialParishLoadDoesNotCreateAnUnusableOfflineProfile()throws Exception{
        var server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);var info=identity();((com.fasterxml.jackson.databind.node.ObjectNode)info.path("user")).put("isGlobal",true);
        server.createContext("/auth/login-multi-tenant",e->reply(e,200,info.toString()));server.createContext("/auth/me",e->reply(e,200,info.toString()));server.createContext("/paroisses",e->reply(e,503,"{}"));server.start();
        String address="http://127.0.0.1:"+server.getAddress().getPort();Path file=temp.resolve("first-login.sqlite");var views=new WorkbenchView[1];var controllers=new WorkbenchController[1];var stage=new javafx.stage.Stage[1];
        try{
            fx(()->{views[0]=new WorkbenchView();controllers[0]=new WorkbenchController(views[0],file,null,address);stage[0]=new javafx.stage.Stage();stage[0].setScene(new javafx.scene.Scene(views[0].root,1100,800));stage[0].show();views[0].username.setText("alice");views[0].password.setText("server password");views[0].localPassword.setText("Local password 1234!");views[0].login.fire();});
            await(()->views[0].status.getText().contains("indisponible")&&!views[0].login.isDisabled());
            assertFalse(java.nio.file.Files.exists(file));
            fx(()->{assertTrue(views[0].workspace.isDisabled());assertTrue(views[0].password.getText().isEmpty());assertTrue(views[0].localPassword.getText().isEmpty());});
        }finally{fx(()->{if(controllers[0]!=null)controllers[0].close();if(stage[0]!=null)stage[0].close();});server.stop(0);}
    }

}
