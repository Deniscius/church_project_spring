package com.missanye.desktop.controller;

import com.missanye.desktop.api.ApiClient;
import com.missanye.desktop.config.DesktopConfiguration;
import com.missanye.desktop.config.ServerSettings;
import com.missanye.desktop.config.ParishDeployment;
import com.missanye.desktop.service.DeviceCredentials;
import com.missanye.desktop.service.ParishLoginService;
import com.missanye.desktop.view.ParishDeploymentDialog;
import com.missanye.desktop.view.ServerSettingsDialog;
import com.missanye.desktop.model.ScreenFormats;
import com.missanye.desktop.model.RequestValidation;
import com.missanye.desktop.service.ReceiptExporter;
import com.missanye.desktop.service.LocalRequestService;
import com.missanye.desktop.service.WorkspaceBootstrap;
import com.missanye.desktop.service.OperationErrors;
import com.missanye.desktop.view.FirstConnectionDialog;
import com.missanye.desktop.repository.LocalStore;
import com.missanye.desktop.service.SyncService;
import com.missanye.desktop.view.WorkbenchView;
import com.missanye.desktop.model.WorkspaceModels.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import javafx.application.*;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public final class WorkbenchController implements AutoCloseable {
    private final WorkbenchView v;
    private final Path file;
    private String apiBaseUrl;
    private boolean parishMode;
    private ParishDeployment deployment;
    private DeviceCredentials deviceCredentials;
    private final HostServices host;
    private final ObjectMapper json=new ObjectMapper();
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"desktop-io");t.setDaemon(true);return t;});
    private final ScheduledExecutorService timer=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"desktop-sync-timer");t.setDaemon(true);return t;});
    private LocalStore store;
    private ApiClient api;
    private char[] reconnectPassword;
    private String reconnectUsername;
    private boolean reconnecting,synchronizing,scheduling,checkingUpdates;
    private boolean settingsRefreshing;
    private final Label dataMessage=new Label("Actualisation automatique toutes les minutes.");
    private long updateCheckedAt;
    private com.missanye.desktop.service.UpdateService.Release latestRelease;
    private final Label updateMessage=new Label("Recherchez les versions publiées de Missanye Desktop.");
    private final javafx.scene.control.Hyperlink updateDownload=new javafx.scene.control.Hyperlink("Installer la mise à jour");
    private final ExecutorService updater=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"desktop-updates");t.setDaemon(true);return t;});
    private final com.missanye.desktop.model.CelebrationPlanner planner=new com.missanye.desktop.model.CelebrationPlanner();
    private Task<?> syncTask;
    private volatile long authEpoch;
    private Task<?> reconnectTask;
    private final ExecutorService reconnector=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"desktop-reconnect");t.setDaemon(true);return t;});
    private JsonNode session,catalog;
    private String editing,correcting;
    private Choice currentParish;
    private ObjectNode cleanForm;
    private volatile boolean closed;
    private long taskGeneration;
    private boolean offlineReady;
    private boolean serverSettingsValid=true;
    private final javafx.animation.PauseTransition readinessDelay=new javafx.animation.PauseTransition(javafx.util.Duration.millis(220));
    private ArrayNode configuredSlots;
    private boolean busy,loading;
    private int transientFailures;
    private long automaticRetryAfter;
    public WorkbenchController(WorkbenchView view,Path file,HostServices host){
        this(view,file,host,DesktopConfiguration.load().apiBaseUrl());
        try{apiBaseUrl=ServerSettings.load(file,apiBaseUrl);}catch(Exception invalid){
            serverSettingsValid=false;v.status.setText("La configuration enregistrée est illisible. Ouvrez Configuration du serveur pour la corriger avant de vous connecter.");
        }
        parishMode=true;deviceCredentials=DeviceCredentials.windows(file);
        try{deployment=ServerSettings.deployment(file).orElse(null);if(deployment!=null)apiBaseUrl=deployment.apiUrl();}catch(Exception invalid){serverSettingsValid=false;}
        v.serverEnvironment.setText(deployment==null?"Ce poste doit être rattaché à une paroisse.":deployment.parishName());
        v.status.setText(deployment==null?"Activez ce poste en choisissant votre paroisse avec vos identifiants habituels.":"Connectez-vous à l'espace de "+deployment.parishName()+".");controls();
    }
    public WorkbenchController(WorkbenchView view,Path file,HostServices host,ParishDeployment selected,DeviceCredentials credentials){
        this(view,file,host,selected.apiUrl());parishMode=true;deployment=selected;deviceCredentials=credentials;v.serverEnvironment.setText(selected.parishName());controls();
    }
    /** Legacy workspace harness retained for migration regression tests; the application loads a parish deployment. */
    public WorkbenchController(WorkbenchView view,Path file,HostServices host,String configuredApiUrl){
        apiBaseUrl=new DesktopConfiguration(configuredApiUrl).apiBaseUrl();
        v=view;this.file=file;this.host=host;
        v.serverEnvironment.setText("Serveur : "+apiBaseUrl);
        v.settings.setOnAction(e->settings());
        for(var entry:java.util.Map.of("navCalendar","celebrations","navCatalog","types","navTreasury","cash","navTeam","team","navProfile","profile").entrySet())v.navigation(entry.getKey()).setOnAction(e->settings(switch(entry.getKey()){case "navCalendar"->hasPermission("dashboard:view")?"celebrations":"programme";case "navCatalog"->hasPermission("request-type:read")?"types":"prices";case "navTreasury"->hasPermission("treasury:read")?"cash":hasPermission("invoice:read")?"invoices":"payments";case "navTeam"->hasPermission("user:manage")?"team":"choirs";default->entry.getValue();}));
        v.configureServer.setOnAction(e->configureServer());v.forgotPassword.setOnAction(e->recoverPassword());
        v.login.setOnAction(e->unlock(true));v.offline.setOnAction(e->unlock(false));v.lock.setOnAction(e->protectForm(this::lock));
        v.parish.valueProperty().addListener((o,a,b)->{if(!closed&&!loading&&!busy)changeParish();});
        v.addDate.setOnAction(e->addDate());v.removeDate.setOnAction(e->{var chosen=v.extraDates.getSelectionModel().getSelectedItem();if(chosen!=null)v.extraDates.getItems().remove(chosen);});
        v.date.setDayCellFactory(picker->new DateCell(){@Override public void updateItem(LocalDate day,boolean empty){super.updateItem(day,empty);if(empty||day==null)return;try{var type=v.type.getValue()==null?json.missingNode():v.type.getValue().data();var plan=v.plan.getValue()==null?json.missingNode():v.plan.getValue().data();setDisable(!planner.allowedStart(type,plan,catalog==null?json.createArrayNode():catalog.path("horaires"),day));}catch(Exception invalid){setDisable(true);}}});
        v.configureSlots.setOnAction(e->configureSlots());
        v.type.setOnAction(e->{if(!loading)fillPlans();});v.plan.setOnAction(e->{price();calendarChanged(true);});v.date.valueProperty().addListener((o,a,b)->calendarChanged(true));
        v.showAction("formError",false);
        v.finalizeRequest.setOnAction(e->finalizeForm());v.fresh.setOnAction(e->protectForm(this::reset));v.save.setOnAction(e->save());v.queue.setOnAction(e->queue());v.edit.setOnAction(e->edit());v.archive.setOnAction(e->archive());v.retry.setOnAction(e->retry());v.returnToDraft.setOnAction(e->returnToDraft());
        v.correct.setOnAction(e->mutate("UPDATE_INTENTION"));v.validate.setOnAction(e->mutate("VALIDATE_DEMANDE"));v.remove.setOnAction(e->mutate("DELETE_DEMANDE"));
        v.refresh.setOnAction(e->refresh());v.sync.setOnAction(e->synchronize());v.cash.setOnAction(e->cash());v.receipt.setOnAction(e->receipt());v.printIntention.setOnAction(e->printIntention());v.copyCode.setOnAction(e->copyDemand("code"));v.copyPhone.setOnAction(e->copyDemand("phone"));v.copyDetails.setOnAction(e->copyDemand("details"));v.checkout.setOnAction(e->checkout());v.reconcile.setOnAction(e->reconcile());
        v.demands.getSelectionModel().selectedItemProperty().addListener((o,a,b)->{v.detail.setText(b==null?"Sélectionnez une demande pour consulter son intention et agir.":description(b.data()));updateActions();});
        v.outbox.getSelectionModel().selectedItemProperty().addListener((o,a,b)->{v.queueDetail.setText(b==null?"Sélectionnez une opération pour consulter son état.":b.data().path("error").asText("Cette opération est enregistrée dans votre espace paroissial.")+"\nSaisi par : "+b.data().path("createdByName").asText("Non renseigné"));updateActions();});
        cleanForm=formData();
        readinessDelay.setOnFinished(e->inspectOfflineReadiness());v.username.textProperty().addListener((o,a,b)->{offlineReady=false;controls();readinessDelay.playFromStart();});
        controls();
        Platform.runLater(()->checkUpdates(false));
        timer.scheduleWithFixedDelay(()->Platform.runLater(()->{if(!closed && !busy && store!=null && ((api!=null)||(api==null&&reconnectPassword!=null)) && !v.hasOpenDialog() && System.currentTimeMillis()>=automaticRetryAfter){if(api==null)reconnect();else{checkUpdates(false);synchronize();}}}),60,60,TimeUnit.SECONDS);
    }
    private <T> void run(Callable<T> action,Consumer<T> success){
        if(busy||closed)return;v.clearError();busy=true;long generation=++taskGeneration;controls();
        var task=new Task<T>(){@Override protected T call()throws Exception{try{return action.call();}finally{if(closed&&store!=null)store.close();}}};
        task.setOnSucceeded(e->{if(closed)return;busy=false;controls();try{success.accept(task.getValue());}catch(Exception x){v.status.setText("Affichage impossible : "+x.getMessage());}});
        task.setOnFailed(e->{if(closed)return;busy=false;
            if(task.getException() instanceof ApiClient.ApiException a && (a.status()==401)){if(api!=null)api.clear();api=null;clearReconnect();v.session.setText("Votre session a expiré ou vos droits ont changé. Déconnectez-vous puis reconnectez-vous pour transmettre votre travail.");}
            Throwable x=task.getException();if(x instanceof SyncService.SessionMismatchException||x instanceof ParishLoginService.ReauthenticationRequired){if(api!=null)api.clear();api=null;clearReconnect();}
            if(store==null){v.showLogin();clearReconnect();}controls();
            if(x instanceof java.io.IOException || (x instanceof ApiClient.ApiException a && (a.status()==429 || a.status()>=500))) {transientFailures=Math.min(transientFailures+1,6);automaticRetryAfter=System.currentTimeMillis()+Math.min(3600000L,60000L*(1L<<transientFailures));}
            v.error(OperationErrors.message(x));
            reloadView(generation);
            if(x instanceof ParishLoginService.LegacyMigrationRequired migration)migrateLegacy(migration);
        });worker.submit(task);
    }
    private void controls(){
        boolean opened=store!=null;v.setWorkspaceOpen(opened);v.workspace.setDisable(busy||!opened);v.lock.setDisable(busy||!opened);
        v.busyIndicator.setVisible(busy);v.busyIndicator.setManaged(busy);
        for(var n:List.of(v.username,v.password,v.localPassword,v.login,v.offline))n.setDisable(busy||opened);
        v.login.setDisable(busy||opened||!serverSettingsValid||(parishMode&&deployment==null));
        if(parishMode){v.parish.setDisable(true);if(deployment!=null)v.parishIdentity(deployment.parishName());}
        v.configureServer.setDisable(busy||opened);
        v.offlinePane.setDisable(busy||opened||!offlineReady||!serverSettingsValid);v.offline.setDisable(busy||opened||!offlineReady||!serverSettingsValid);
        v.refresh.setDisable((api==null&&reconnectPassword==null)||reconnecting||synchronizing);v.sync.setDisable((api==null&&reconnectPassword==null)||reconnecting||synchronizing);updateActions();
    }
    private void configureServer(){
        if(busy||store!=null)return;
        if(parishMode){
            try{ParishDeploymentDialog.show(v,file,apiBaseUrl).ifPresent(selected->{deployment=selected;apiBaseUrl=selected.apiUrl();serverSettingsValid=true;v.serverEnvironment.setText(selected.parishName());v.password.clear();v.localPassword.clear();offlineReady=false;v.status.setText("Poste configuré pour "+selected.parishName()+". Connectez-vous avec votre compte habituel.");controls();});}
            catch(Exception failure){v.status.setText("Le rattachement du poste n'a pas pu être effectué.");}return;
        }
        try{ServerSettingsDialog.show(v,file,apiBaseUrl).ifPresent(address->{
            readinessDelay.stop();apiBaseUrl=address;v.serverEnvironment.setText("Serveur : "+apiBaseUrl);serverSettingsValid=true;v.password.clear();v.localPassword.clear();offlineReady=false;transientFailures=0;automaticRetryAfter=0;
            v.status.setText("Serveur enregistré. Connectez-vous avec votre compte de cet environnement.");controls();inspectOfflineReadiness();
        });}catch(Exception failure){v.status.setText("Impossible d'ouvrir la configuration du serveur. Contactez votre administrateur.");}
    }
    private String parish(){if(v.parish.getValue()==null)throw new IllegalArgumentException("Sélectionnez une paroisse.");return v.parish.getValue().id();}
    private void inspectOfflineReadiness(){
        if(closed||store!=null)return;String username=v.username.getText().trim();
        if(username.isBlank()){offlineReady=false;v.offlineAvailability.setText("Une connexion Internet est nécessaire lors de la première utilisation sur ce poste.");controls();return;}
        String inspectedServer=apiBaseUrl;
        worker.submit(()->{
            LocalStore.ProfileStatus status;
            try{status=LocalStore.inspect(file,inspectedServer,username);}catch(Exception e){status=LocalStore.ProfileStatus.NEEDS_ONLINE;}
            final var result=status;Platform.runLater(()->{if(closed||store!=null||!inspectedServer.equals(apiBaseUrl)||!username.equals(v.username.getText().trim()))return;offlineReady=result==LocalStore.ProfileStatus.READY;
                v.offlineAvailability.setText(offlineReady?"Ce compte dispose d'un espace préparé sur ce poste.":result==LocalStore.ProfileStatus.ABSENT?"Première utilisation : connectez-vous en ligne pour préparer ce poste.":"La préparation de ce compte doit être terminée en ligne.");controls();});
        });
    }
    private record Authenticated(ApiClient api,JsonNode session,JsonNode parishes,boolean firstProfile,String username){}
    private record Opened(ApiClient api,LocalStore store,JsonNode session,JsonNode parishes,String selected,boolean preparedNow){}
    private SyncService syncService(){return new SyncService(store,api,parishMode&&deployment!=null?deployment.parishId():null);}
    private void migrateLegacy(ParishLoginService.LegacyMigrationRequired migration){
        var dialog=v.decorate(new Dialog<char[]>());dialog.setTitle("Retrouver vos brouillons existants");dialog.setHeaderText("Mise à jour de la protection de ce poste");
        var secret=new PasswordField();secret.setPromptText("Ancien mot de passe local du poste");var content=new javafx.scene.layout.VBox(12,new Label("Une seule fois, saisissez l'ancien mot de passe local pour conserver votre travail. Les prochaines connexions utiliseront uniquement vos identifiants habituels."),secret);dialog.getDialogPane().setContent(content);
        var apply=new ButtonType("Conserver mon travail",ButtonBar.ButtonData.OK_DONE);dialog.getDialogPane().getButtonTypes().addAll(new ButtonType("Annuler",ButtonBar.ButtonData.CANCEL_CLOSE),apply);dialog.setResultConverter(button->button==apply?secret.getText().toCharArray():null);
        var selected=dialog.showAndWait();secret.clear();selected.ifPresent(password->run(()->{ParishLoginService.migrate(file,deployment,deviceCredentials,migration,password);return true;},done->v.status.setText("Vos brouillons sont conservés. Connectez-vous avec votre mot de passe habituel.")));
    }
    private void normalLogin(){
        if(deployment==null||busy||!serverSettingsValid)return;String username=v.username.getText().strip();char[] password=v.password.getText().toCharArray();v.password.clear();
        if(username.isBlank()||password.length==0){Arrays.fill(password,'\0');v.status.setText("Saisissez votre identifiant et votre mot de passe.");return;}
        clearReconnect();reconnectUsername=username;reconnectPassword=password.clone();
        v.status.setText("Connexion à l'espace de "+deployment.parishName()+"…");v.showPreparation(deployment.parishName());
        run(()->ParishLoginService.login(file,deployment,deviceCredentials,username,password),result->{
            if(result.api()!=null)clearReconnect();
            finishOpen(new Opened(result.api(),result.store(),result.session(),result.parishes(),result.parish(),true));
            v.session.setText(result.session().path("user").path("username").asText()+" — "+result.session().path("user").path("role").asText());
            v.status.setText("Bienvenue dans l'espace de "+deployment.parishName()+".");
        });
    }
    private void unlock(boolean online){
        if(parishMode){normalLogin();return;}
        String username=v.username.getText().trim(),password=v.password.getText();
        if(username.isBlank()||(online&&password.isEmpty())){v.status.setText("Saisissez votre identifiant et votre mot de passe.");return;}
        readinessDelay.stop();String providedLocal=v.localPassword.getText();v.password.clear();v.localPassword.clear();
        if(!online){
            if(providedLocal.isEmpty()){v.status.setText("Saisissez votre mot de passe local pour ouvrir votre espace hors connexion.");return;}
            char[] local=providedLocal.toCharArray();
            run(()->{LocalStore opened=null;try{
                opened=LocalStore.unlock(file,apiBaseUrl,username,local,null);var info=opened.get("session");
                if(!opened.owner().equals(info.path("user").path("id").asText()))throw new IllegalArgumentException("Une connexion en ligne est nécessaire pour terminer la préparation de cet espace.");
                var ready=json.createArrayNode();for(var parish:WorkspaceBootstrap.eligible(opened.get("parishes")))if(opened.workspaceReady(WorkspaceBootstrap.parishId(parish)))ready.add(parish);
                if(ready.isEmpty())throw new IllegalArgumentException("Connectez-vous en ligne pour préparer les données de votre paroisse sur ce poste.");
                String selected=opened.get("selected-parish").asText(WorkspaceBootstrap.parishId(ready.get(0)));if(!WorkspaceBootstrap.allows(ready,selected))selected=WorkspaceBootstrap.parishId(ready.get(0));
                return new Opened(null,opened,info,ready,selected,false);
            }catch(Exception e){if(opened!=null)opened.close();throw e;}finally{Arrays.fill(local,'\0');}},this::finishOpen);
            return;
        }
        v.status.setText("Connexion au service et vérification de vos accès… Le premier démarrage du serveur peut prendre jusqu’à deux minutes.");
        run(()->{
            var client=new ApiClient(apiBaseUrl);try{
                var info=client.login(username,password);if(info.path("user").path("id").asText().isBlank())throw new IllegalStateException("Session sans propriétaire");
                var parishes=WorkspaceBootstrap.eligible(info.path("user").path("isGlobal").asBoolean()?client.get("paroisses"):info.path("paroisses"));
                if(parishes.isEmpty()&&!info.path("user").path("isGlobal").asBoolean())throw new IllegalArgumentException("Aucune paroisse active n'est accessible avec ce compte. Contactez votre administrateur.");
                return new Authenticated(client,info,parishes,LocalStore.inspect(file,apiBaseUrl,username)==LocalStore.ProfileStatus.ABSENT,username);
            }catch(Exception e){client.clear();throw e;}
        },authenticated->{
            try{
                if(authenticated.parishes().isEmpty()){
                    authenticated.api().clear();v.status.setText("Connexion réussie : compte administrateur vérifié. Aucune paroisse active n'est encore disponible sur ce serveur.");
                    var notice=v.decorate(new Alert(Alert.AlertType.INFORMATION,"Votre compte administrateur est reconnu par le serveur. Créez ou activez une paroisse depuis l'interface web de cet environnement, puis reconnectez-vous pour préparer ce poste.",new ButtonType("Compris",ButtonBar.ButtonData.OK_DONE)));
                    notice.setTitle("Compte administrateur connecté");notice.setHeaderText("Connexion réussie — aucune paroisse à préparer");notice.showAndWait();inspectOfflineReadiness();return;
                }
                FirstConnectionDialog.Selection selection;
                if(!authenticated.firstProfile()&&!providedLocal.isEmpty())selection=new FirstConnectionDialog.Selection(null,providedLocal.toCharArray());
                else {var chosen=FirstConnectionDialog.show(v,authenticated.session().path("user"),authenticated.parishes(),authenticated.firstProfile());if(chosen.isEmpty()){authenticated.api().clear();v.status.setText("Connexion annulée. Votre espace local n'a pas été modifié.");inspectOfflineReadiness();return;}selection=chosen.get();}
                prepareOnline(authenticated,selection);
            }catch(Exception e){authenticated.api().clear();v.status.setText("Impossible de préparer la connexion. Votre travail précédent est conservé.");inspectOfflineReadiness();}
        });
    }
    private void prepareOnline(Authenticated authenticated,FirstConnectionDialog.Selection selection){
        String display=selection.parish()==null?"Votre paroisse":selection.parish().label();v.showPreparation(display);v.status.setText("Préparation de votre espace : patientez jusqu'à la fin du chargement.");
        run(()->{
            LocalStore opened=null;char[] secret=selection.password();try{
                String selected;boolean preparedNow=false;
                if(authenticated.firstProfile()){
                    selected=selection.parish().id();var prepared=WorkspaceBootstrap.fetch(authenticated.api(),authenticated.session(),selected);
                    opened=LocalStore.unlock(file,apiBaseUrl,authenticated.username(),secret,authenticated.session().path("user").path("id").asText());
                    opened.installWorkspace(prepared.session(),authenticated.parishes(),selected,prepared);preparedNow=true;
                }else{
                    opened=LocalStore.unlock(file,apiBaseUrl,authenticated.username(),secret,authenticated.session().path("user").path("id").asText());
                    selected=opened.get("selected-parish").asText(WorkspaceBootstrap.parishId(authenticated.parishes().get(0)));
                    if(!WorkspaceBootstrap.allows(authenticated.parishes(),selected))selected=WorkspaceBootstrap.parishId(authenticated.parishes().get(0));
                    opened.saveSession(authenticated.session(),authenticated.parishes());
                    if(!opened.workspaceReady(selected)){var prepared=WorkspaceBootstrap.fetch(authenticated.api(),authenticated.session(),selected);opened.installWorkspace(prepared.session(),authenticated.parishes(),selected,prepared);preparedNow=true;}
                }
                return new Opened(authenticated.api(),opened,opened.get("session"),authenticated.parishes(),selected,preparedNow);
            }catch(Exception e){authenticated.api().clear();if(opened!=null)opened.close();throw e;}finally{Arrays.fill(secret,'\0');}
        },this::finishOpen);
    }
    private void finishOpen(Opened opened){
        api=opened.api();store=opened.store();session=opened.session();offlineReady=true;loading=true;v.parish.getItems().clear();
        for(var p:opened.parishes())v.parish.getItems().add(new Choice(WorkspaceBootstrap.parishId(p),p.path("nom").asText(p.path("paroisseNom").asText()),p));
        for(var p:v.parish.getItems())if(p.id().equals(opened.selected())){v.parish.setValue(p);break;}
        loading=false;v.session.setText(session.path("user").path("username").asText(v.username.getText()));controls();
        v.status.setText("Connexion réussie. Votre espace paroissial est prêt.");
        openParish(v.parish.getValue(),!opened.preparedNow());if(api!=null)checkUpdates(false);
    }
    private void lock(){
        cancelReconnect();clearReconnect();
        var client=api;run(()->{try{if(client!=null)client.logout();}catch(Exception ignored){}finally{if(store!=null)store.close();}return true;},ignored->{store=null;api=null;session=null;catalog=null;editing=null;currentParish=null;v.parish.getItems().clear();v.demandRows.clear();v.activityRows.clear();v.resetDemandFilters();v.queueFilter.getSelectionModel().selectFirst();v.type.getItems().clear();v.plan.getItems().clear();v.schedule.getItems().clear();v.payment.getItems().clear();reset();v.detail.setText("");v.queueDetail.setText("");v.session.setText(parishMode?"Déconnecté":"Coffre verrouillé");v.status.setText("Vous êtes déconnecté. Votre travail reste protégé sur ce poste.");controls();inspectOfflineReadiness();});
    }
    private record Snapshot(JsonNode catalogue,JsonNode demands,List<ObjectNode> entries,JsonNode session){}
    private Snapshot snapshot(String parish)throws Exception{return new Snapshot(store.get("catalogue:"+parish),new LocalRequestService(store).overlay(parish,store.get("demands:"+parish)),store.entries(),store.get("session"));}
    private void reloadView(long generation){
        LocalStore captured=store;Choice parish=currentParish;if(captured==null||parish==null)return;
        worker.submit(()->{try{var cached=snapshot(parish.id());Platform.runLater(()->{if(!closed&&!busy&&taskGeneration==generation&&store==captured){display(cached);controls();}});}catch(Exception ignored){/* Preserve the original operation error. */}});
    }
    private void changeParish(){
        if(store==null||v.parish.getValue()==null)return;Choice requested=v.parish.getValue();
        if(currentParish!=null&&!currentParish.id().equals(requested.id())){
            loading=true;v.parish.setValue(currentParish);loading=false;protectForm(()->openParish(requested));
        }else openParish(requested);
    }
    private record ContextLoaded(Snapshot snapshot,boolean prepared){}
    private void openParish(Choice selected){openParish(selected,true);}
    private void openParish(Choice selected,boolean refreshAfter){
        loading=true;v.parish.setValue(selected);loading=false;currentParish=selected;catalog=null;
        loading=true;v.type.getItems().clear();v.plan.getItems().clear();v.schedule.getItems().clear();v.payment.getItems().clear();loading=false;reset();
        v.demandRows.clear();v.activityRows.clear();
        run(()->{
            boolean prepared=false;
            if(!store.workspaceReady(selected.id())){
                if(api==null)throw new IllegalStateException("Connectez-vous à Internet pour préparer cette paroisse.");
                var data=WorkspaceBootstrap.fetch(api,session,selected.id());
                store.installWorkspace(data.session(),store.get("parishes"),selected.id(),data);prepared=true;
            }
            store.selectWorkspace(selected.id());
            return new ContextLoaded(snapshot(selected.id()),prepared);
        },s->{display(s.snapshot());if(api!=null&&refreshAfter&&!s.prepared())refresh();});
    }
    private void display(Snapshot s){
        boolean dirty=isDirty();String oldType=id(v.type),oldPlan=id(v.plan),oldSchedule=id(v.schedule),oldPayment=id(v.payment);
        if(!Objects.equals(catalog,s.catalogue())){
            catalog=s.catalogue();loading=true;
            setChoices(v.type,catalog.path("types"),"libelle");if(dirty||!oldType.isBlank())choose(v.type,oldType);
            setChoices(v.schedule,catalog.path("horaires"),"libelle");if(dirty||!oldSchedule.isBlank())choose(v.schedule,oldSchedule);
            setChoices(v.payment,catalog.path("paiements"),"libelle");if(dirty||!oldPayment.isBlank())choose(v.payment,oldPayment);
            fillPlans();if(dirty||!oldPlan.isBlank())choose(v.plan,oldPlan);loading=false;price();calendarChanged(!dirty);
        }
        if(s.session().path("user").path("id").asText().equals(store.owner())){session=s.session();if(parishMode)v.session.setText(session.path("user").path("username").asText()+" — "+session.path("user").path("role").asText());}
        if(!dirty)cleanForm=formData();
        String demandSelection=selectedKey(v.demands,"publicId"),activitySelection=selectedKey(v.outbox,"id");
        var demands=new ArrayList<Row>();for(var d:s.demands())demands.add(new Row(d,description(d)));v.demandRows.setAll(demands);restoreSelection(v.demands,"publicId",demandSelection);
        var activities=new ArrayList<Row>();for(var e:s.entries())if(!"ARCHIVED".equals(e.path("state").asText())) {
            JsonNode p=e.path("payload");String associated=p.path("paroissePublicId").asText(e.path("parish").asText());
            if(associated.isBlank() || associated.equals(parish()))activities.add(new Row(e,stateLabel(e.path("state").asText())+" — "+kindLabel(e.path("kind").asText())+" — "+ScreenFormats.reference(e)));
        }
        v.activityRows.setAll(activities);restoreSelection(v.outbox,"id",activitySelection);updateActions();
    }
    private String selectedKey(TableView<Row> table,String field){var row=table.getSelectionModel().getSelectedItem();return row==null?"":row.data().path(field).asText();}
    private void restoreSelection(TableView<Row> table,String field,String key){table.getSelectionModel().clearSelection();if(key.isBlank())return;for(var row:table.getItems())if(key.equals(row.data().path(field).asText())){table.getSelectionModel().select(row);break;}}
    private boolean hasPermission(String permission){if(session!=null)for(var p:session.path("user").path("permissions"))if(permission.equals(p.asText()))return true;return false;}
    private void updateActions(){
        if(parishMode&&session!=null){
            boolean edit=hasPermission("demand:edit");
            for(var node:List.of(v.finalizeRequest,v.save,v.fresh,v.correct,v.edit,v.queue,v.returnToDraft)){node.setVisible(edit);node.setManaged(edit);}
            for(String id:List.of("navRequest","homeRequest"))v.showAction(id,edit);
            boolean mutations=edit||hasPermission("payment:manage")||hasPermission("demand:validate")||hasPermission("demand:delete");v.sync.setVisible(false);v.sync.setManaged(false);v.auto.setVisible(false);v.auto.setManaged(false);v.auto.setSelected(mutations);v.retry.setVisible(mutations);v.retry.setManaged(mutations);v.archive.setVisible(mutations);v.archive.setManaged(mutations);if(!mutations)v.auto.setSelected(false);
            boolean payments=hasPermission("payment:manage");for(var node:List.of(v.cash,v.checkout,v.reconcile)){node.setVisible(payments);node.setManaged(payments);}
            v.validate.setVisible(hasPermission("demand:validate"));v.validate.setManaged(v.validate.isVisible());v.remove.setVisible(hasPermission("demand:delete"));v.remove.setManaged(v.remove.isVisible());
            v.showAction("navCalendar",hasPermission("dashboard:view")||hasPermission("schedule:read"));
            v.showAction("navCatalog",hasPermission("request-type:read")||hasPermission("pricing:read"));
            v.showAction("navTreasury",hasPermission("treasury:read")||hasPermission("invoice:read")||hasPermission("payment:read"));
            v.showAction("navTeam",hasPermission("user:manage")||hasPermission("choir:read"));v.showAction("navProfile",hasPermission("profile:read"));
            boolean receipt=hasPermission("invoice:read")||hasPermission("receipt:manage");v.receipt.setVisible(receipt);v.receipt.setManaged(receipt);
        }

        boolean ready=store!=null&&!busy;var demand=v.demands.getSelectionModel().getSelectedItem();var activity=v.outbox.getSelectionModel().getSelectedItem();
        boolean paid=demand!=null&&"PAYE".equals(demand.data().path("statutPaiement").asText());
        v.correct.setDisable(!ready||demand==null||demand.data().path("localPending").asBoolean()||!hasPermission("demand:edit"));
        v.validate.setDisable(!ready||demand==null||!hasPermission("demand:validate")||!demand.data().path("validationRequise").asBoolean()||demand.data().path("localDecisionRecorded").asBoolean()||demand.data().path("localReconciliationRequired").asBoolean());
        v.remove.setDisable(!ready||demand==null||paid||demand.data().path("localPending").asBoolean()||!hasPermission("demand:delete"));
        boolean waiting=demand!=null&&v.activityRows.stream().anyMatch(row->Set.of("PENDING","BLOCKED").contains(row.data().path("state").asText())&&!("VALIDATE_DEMANDE".equals(row.data().path("kind").asText())&&"VALIDEE".equals(row.data().path("payload").path("statut").asText()))&&demand.data().path("publicId").asText().equals(row.data().path("payload").path("demandePublicId").asText()));
        v.cash.setDisable(!ready||demand==null||paid||demand.data().path("localCashReceived").asBoolean()||waiting||!hasPermission("payment:manage")||(demand.data().has("paiementDisponible")&&!demand.data().path("paiementDisponible").asBoolean()));
        v.printIntention.setDisable(!ready||!hasPermission("demand:read"));v.copyActions.setDisable(!ready||demand==null);
        v.receipt.setDisable(!ready||demand==null);v.checkout.setDisable(!ready||api==null||demand==null||paid||demand.data().path("localPending").asBoolean());v.reconcile.setDisable(!ready||api==null||demand==null||demand.data().path("localPending").asBoolean());
        boolean financial=activity!=null&&("CASH_PAYMENT".equals(activity.data().path("kind").asText())||v.activityRows.stream().anyMatch(row->"CASH_PAYMENT".equals(row.data().path("kind").asText())&&(activity.data().path("id").asText().equals(row.data().path("localRequestId").asText())||activity.data().path("id").asText().equals(row.data().path("dependsOn").asText()))));
        String state=activity==null?"":activity.data().path("state").asText();boolean editable=activity!=null&&!financial&&"CREATE_DEMANDE".equals(activity.data().path("kind").asText())&&(state.equals("DRAFT")||state.equals("REJECTED"));
        v.queue.setDisable(!ready||!state.equals("DRAFT")||!hasPermission("demand:edit"));v.edit.setDisable(!ready||!editable);v.archive.setDisable(!ready||financial||!(state.equals("DRAFT")||state.equals("REJECTED")));v.retry.setDisable(!ready||api==null||!state.equals("BLOCKED"));v.returnToDraft.setDisable(!ready||activity==null||activity.data().has("localSnapshot")||!state.equals("PENDING")||!"CREATE_DEMANDE".equals(activity.data().path("kind").asText())||activity.data().path("attempts").asInt()!=0);
    }
    private String stateLabel(String state){return ScreenFormats.state(state);}
    private String kindLabel(String kind){return switch(kind){case "CREATE_DEMANDE"->"Nouvelle demande";case "CASH_PAYMENT"->"Encaissement espèces";case "UPDATE_INTENTION"->"Correction d'intention";case "VALIDATE_DEMANDE"->"Décision";case "DELETE_DEMANDE"->"Suppression";default->kind;};}
    private void mutate(String kind){try{
        requirePermission(switch(kind){case "UPDATE_INTENTION"->"demand:edit";case "VALIDATE_DEMANDE"->"demand:validate";default->"demand:delete";});
        var d=selected(v.demands);String text=null,decision=null;
        if("UPDATE_INTENTION".equals(kind)){var value=editIntention(d.path("intention").asText());if(value.isEmpty())return;text=value.get();}
        else if("VALIDATE_DEMANDE".equals(kind)){var dialog=v.decorate(new ChoiceDialog<>("Valider",List.of("Valider","Rejeter")));dialog.setTitle("Décision sur la demande");dialog.setHeaderText(d.path("codeSuivie").asText());var value=dialog.showAndWait();if(value.isEmpty())return;decision="Valider".equals(value.get())?"VALIDEE":"REJETEE";}
        if(!confirm("Préparer cette opération pour "+d.path("codeSuivie").asText()+" ? Une vérification protégera les éventuelles modifications effectuées entre-temps."))return;
        if("VALIDATE_DEMANDE".equals(kind)&&d.path("publicId").asText().startsWith("local:")){
            String local=d.path("localRequestId").asText(),chosen=decision,context=parish();run(()->{new LocalRequestService(store).decide(local,chosen);return snapshot(context);},s->{display(s);v.status.setText("Décision enregistrée.");});return;
        }
        String intention=text,statut=decision,parish=parish();run(()->{store.queueMutation(kind,d,intention,statut);return snapshot(parish);},s->{display(s);v.tabs.getSelectionModel().select(2);v.status.setText("Opération enregistrée.");});
    }catch(Exception e){v.error(e.getMessage());}}
    private String description(JsonNode d){return (d.path("localReconciliationRequired").asBoolean()?"À RAPPROCHER — ":"")+ScreenFormats.reference(d)+" · "+ScreenFormats.faithful(d)+" · "+ScreenFormats.money(d.path("montant"))+" · "+ScreenFormats.state(d.path("statutPaiement").asText())+"\n"+d.path("intention").asText();}
    private void setChoices(ComboBox<Choice> box,JsonNode values,String label){box.getItems().clear();for(var value:values)if(value.path("isActive").asBoolean(true) && !value.path("statusDel").asBoolean()){String text=value.path(label).asText(value.path("publicId").asText());if(value.has("heureCelebration"))text+=" — "+value.path("jourSemaine").asText()+" "+value.path("heureCelebration").asText();box.getItems().add(new Choice(value.path("publicId").asText(),text,value));}if(!box.getItems().isEmpty())box.getSelectionModel().selectFirst();}
    private void fillPlans(){if(catalog==null || v.type.getValue()==null){v.plan.getItems().clear();return;}setChoices(v.plan,catalog.path("forfaits").path(v.type.getValue().id()),"nomForfait");price();}
    private void calendarChanged(boolean regenerate){
        if(loading||scheduling||catalog==null||v.type.getValue()==null||v.plan.getValue()==null||v.date.getValue()==null)return;
        scheduling=true;
        try{
            var type=v.type.getValue().data();var plan=v.plan.getValue().data();
            var preferred=regenerate?planner.preferredPlan(catalog.path("forfaits").path(id(v.type)),plan,catalog.path("horaires"),v.date.getValue()):plan;
            if(!preferred.path("publicId").asText().equals(id(v.plan))){choose(v.plan,preferred.path("publicId").asText());plan=preferred;price();v.status.setText("Forfait adapté au calendrier : "+v.plan.getValue().label()+". Vérifiez le montant avant de finaliser.");}
            boolean custom=com.missanye.desktop.model.CelebrationPlanner.customAllowed(type,plan);v.customTime.setDisable(!custom);v.showAction("customTimeLabel",custom);v.customTime.setVisible(custom);v.customTime.setManaged(custom);javafx.scene.layout.GridPane.setColumnIndex(v.configureSlots,custom?1:0);javafx.scene.layout.GridPane.setColumnSpan(v.configureSlots,custom?1:2);if(!custom)v.customTime.clear();
            String selected=id(v.schedule);var options=planner.schedules(catalog.path("horaires"),v.date.getValue(),type,plan);var rows=json.createArrayNode();options.forEach(rows::add);setChoices(v.schedule,rows,"libelle");for(var choice:v.schedule.getItems())if(choice.id().equals(selected)){v.schedule.setValue(choice);break;}
            if(regenerate){
                var dates=plan.path("nombreCelebration").asInt(1)==1?List.of(v.date.getValue()):planner.dates(v.date.getValue(),type,plan);
                v.extraDates.getItems().setAll(dates.stream().skip(1).toList());var slots=json.createArrayNode();
                for(var date:dates){var slot=slots.addObject().put("date",date.toString());var available=planner.schedules(catalog.path("horaires"),date,type,plan);var chosen=available.stream().filter(h->id(v.schedule).equals(h.path("publicId").asText())).findFirst().orElse(available.isEmpty()?null:available.get(0));if(chosen!=null)slot.put("horairePublicId",chosen.path("publicId").asText());}
                configuredSlots=slots;v.slotsSummary.setText(dates.size()+" célébration(s) — horaires adaptés au programme paroissial.");
            }
            if(options.isEmpty()&&!custom)v.slotsSummary.setText("Aucun horaire disponible pour cette date et ce forfait. Choisissez une autre date.");
        }catch(IllegalArgumentException invalid){v.slotsSummary.setText(invalid.getMessage());v.status.setText(invalid.getMessage());configuredSlots=null;}
        finally{scheduling=false;}
    }
    private void requirePermission(String permission){
        if(session!=null)for(var p:session.path("user").path("permissions"))if(permission.equals(p.asText()))return;
        throw new IllegalArgumentException("Vous ne disposez pas des droits nécessaires pour cette action.");
    }
    private void price(){v.price.setText(v.plan.getValue()==null?"Aucun forfait disponible":"Montant : "+ScreenFormats.money(v.plan.getValue().data().path("montantForfait")));}
    private void reset(){editing=null;correcting=null;configuredSlots=null;v.slotsSummary.setText("Horaire commun par défaut");v.surname.clear();v.firstname.clear();v.phone.clear();v.email.clear();v.courier.clear();v.intention.clear();v.extraDates.getItems().clear();v.customTime.clear();v.date.setValue(LocalDate.now().plusDays(2));v.date.getEditor().setText(v.date.getConverter().toString(v.date.getValue()));v.additionalDate.setValue(v.date.getValue().plusDays(1));calendarChanged(true);cleanForm=formData();}
    private String required(ComboBox<Choice> box,String label){if(box.getValue()==null)throw new IllegalArgumentException(label+" obligatoire.");return box.getValue().id();}
    private String id(ComboBox<Choice> choice){return choice.getValue()==null?"":choice.getValue().id();}
    private ObjectNode formData(){
        var p=json.createObjectNode().put("paroissePublicId",currentParish==null?"":currentParish.id()).put("typeDemandePublicId",id(v.type)).put("forfaitTarifPublicId",id(v.plan)).put("typePaiementPublicId",id(v.payment)).put("horairePublicId",id(v.schedule));
        LocalDate first=null;String typedDate=v.date.getEditor().getText();
        try{if(typedDate!=null&&!typedDate.isBlank())first=v.date.getConverter().fromString(typedDate);}catch(Exception invalid){p.put("_invalidDateText",typedDate);}
        p.put("nomFidele",v.surname.getText()).put("prenomFidele",v.firstname.getText()).put("telFidele",v.phone.getText().trim()).put("emailFidele",v.email.getText().trim()).put("nomCoursier",v.courier.getText()).put("intention",v.intention.getText()).put("dateDebut",first==null?"":first.toString()).put("_customTimeText",v.customTime.getText());
        p.put("_expectedCelebrations",v.plan.getValue()==null?0:v.plan.getValue().data().path("nombreCelebration").asInt(1));
        var dates=new TreeSet<LocalDate>();if(first!=null)dates.add(first);dates.addAll(v.extraDates.getItems());var list=p.putArray("datesCelebration");dates.forEach(d->list.add(d.toString()));
        if(configuredSlots!=null){var slots=configuredSlots.deepCopy();for(var slot:slots)if(first!=null&&first.toString().equals(slot.path("date").asText())){var row=(ObjectNode)slot;row.remove(List.of("horairePublicId","heurePersonnalisee"));if(!v.customTime.getText().isBlank())row.put("heurePersonnalisee",v.customTime.getText().trim());else if(v.schedule.getValue()!=null)row.put("horairePublicId",id(v.schedule));}p.set("celebrationSlots",slots);}return p;
    }
    public boolean isDirty(){return store!=null&&cleanForm!=null&&!cleanForm.equals(formData());}
    public void requestClose(Runnable close){if(busy){v.status.setText("Une opération est en cours. Attendez sa fin avant de fermer l'application.");return;}protectForm(close);}
    private void protectForm(Runnable next){
        if(busy||closed)return;if(!isDirty()){next.run();return;}
        var save=new ButtonType("Enregistrer le brouillon",ButtonBar.ButtonData.OK_DONE);var discard=new ButtonType("Abandonner la saisie",ButtonBar.ButtonData.NO);
        var dialog=v.decorate(new Alert(Alert.AlertType.CONFIRMATION,"Votre demande contient des informations non enregistrées.",save,discard,ButtonType.CANCEL));dialog.setTitle("Conserver votre travail");dialog.setHeaderText("Enregistrer avant de continuer ?");
        var answer=dialog.showAndWait().orElse(ButtonType.CANCEL);if(answer==save)saveDraft(next);else if(answer==discard){reset();next.run();}
    }
    private LocalTime parseTime(String value){try{return LocalTime.parse(value);}catch(java.time.format.DateTimeParseException e){throw new IllegalArgumentException("Saisissez une heure valide, par exemple 06:30.");}}
    private void addDate(){
        LocalDate selected=v.additionalDate.getValue();
        if(selected==null){v.status.setText("Choisissez une date de célébration.");return;}
        if(selected.equals(v.date.getValue())||v.extraDates.getItems().contains(selected)){v.status.setText("Cette date est déjà sélectionnée.");return;}
        if(v.extraDates.getItems().size()>=29){v.status.setText("Une demande peut comporter au maximum 30 célébrations.");return;}
        v.extraDates.getItems().add(selected);javafx.collections.FXCollections.sort(v.extraDates.getItems());v.additionalDate.setValue(selected.plusDays(1));
    }
    private Optional<String> editIntention(String original){
        var dialog=v.decorate(new Dialog<String>());dialog.setTitle("Corriger l'intention");dialog.setHeaderText("Correction envoyée à la prochaine synchronisation");
        var text=new TextArea(original);text.setId("intentionEditorText");text.setWrapText(true);text.setPrefRowCount(5);var hint=new Label();hint.setId("intentionLength");
        dialog.getDialogPane().setContent(new javafx.scene.layout.VBox(12,new Label("Intention de messe"),text,hint));
        var apply=new ButtonType("Enregistrer la correction",ButtonBar.ButtonData.OK_DONE);dialog.getDialogPane().getButtonTypes().addAll(new ButtonType("Annuler",ButtonBar.ButtonData.CANCEL_CLOSE),apply);
        Runnable validate=()->{int length=text.getText().trim().length();hint.setText(length+" / 500 caractères — minimum 10");dialog.getDialogPane().lookupButton(apply).setDisable(length<10||length>500);};
        text.textProperty().addListener((o,a,b)->validate.run());validate.run();dialog.setResultConverter(button->button==apply?text.getText().trim():null);return dialog.showAndWait();
    }
    private void configureSlots(){try{
        if(v.date.getValue()==null)throw new IllegalArgumentException("Date obligatoire");
        var dates=new TreeSet<LocalDate>();dates.add(v.date.getValue());dates.addAll(v.extraDates.getItems());
        if(dates.size()>30)throw new IllegalArgumentException("30 dates au maximum.");
        var dialog=v.decorate(new Dialog<ArrayNode>());dialog.setTitle("Créneaux de célébration");dialog.setHeaderText("Choisir l'horaire ou l'heure personnalisée pour chaque date.");
        var apply=new ButtonType("Appliquer les créneaux",ButtonBar.ButtonData.OK_DONE);dialog.getDialogPane().getButtonTypes().addAll(new ButtonType("Annuler",ButtonBar.ButtonData.CANCEL_CLOSE),apply);
        var grid=new javafx.scene.layout.GridPane();grid.setHgap(12);grid.setVgap(12);
        for(double percent:new double[]{24,38,38}){var column=new javafx.scene.layout.ColumnConstraints();column.setPercentWidth(percent);grid.getColumnConstraints().add(column);}
        grid.addRow(0,new Label("Date"),new Label("Horaire"),new Label("Heure personnalisée"));
        var choices=new ArrayList<ComboBox<Choice>>();var hours=new ArrayList<TextField>();int row=1;
        for(var date:dates){var box=new ComboBox<Choice>();box.setMaxWidth(Double.MAX_VALUE);box.setMinWidth(0);var available=json.createArrayNode();planner.schedules(catalog.path("horaires"),date,v.type.getValue().data(),v.plan.getValue().data()).forEach(available::add);setChoices(box,available,"libelle");var time=new TextField(v.customTime.getText());time.setId("slotTime"+(row-1));time.setMinWidth(0);time.setPromptText("HH:mm");boolean allowCustom=com.missanye.desktop.model.CelebrationPlanner.customAllowed(v.type.getValue().data(),v.plan.getValue().data());time.setDisable(!allowCustom);if(!allowCustom)time.clear();grid.addRow(row++,new Label(date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu"))),box,time);choices.add(box);hours.add(time);}
        var error=new Label();error.setId("slotValidation");error.setStyle("-fx-text-fill: #8a3325;");dialog.getDialogPane().setContent(new javafx.scene.layout.VBox(12,grid,error));
        ArrayNode[] prepared={null};dialog.getDialogPane().lookupButton(apply).addEventFilter(javafx.event.ActionEvent.ACTION,event->{
            try{var slots=json.createArrayNode();int i=0;for(var date:dates){var slot=slots.addObject().put("date",date.toString());String time=hours.get(i).getText().trim();if(!time.isBlank()){if(!com.missanye.desktop.model.CelebrationPlanner.customAllowed(v.type.getValue().data(),v.plan.getValue().data()))throw new IllegalArgumentException("Heure personnalisée réservée aux demandes spéciales autorisées.");slot.put("heurePersonnalisee",parseTime(time).toString());}else slot.put("horairePublicId",required(choices.get(i),"Horaire du "+date));i++;}prepared[0]=slots;}
            catch(Exception invalid){event.consume();error.setText("Corrigez le créneau : "+invalid.getMessage());}
        });dialog.setResultConverter(button->button==apply?prepared[0]:null);
        var result=dialog.showAndWait();if(result.isPresent()){configuredSlots=result.get();var first=configuredSlots.get(0);v.customTime.setText(first.path("heurePersonnalisee").asText());choose(v.schedule,first.path("horairePublicId").asText());v.slotsSummary.setText(configuredSlots.size()+" créneau(x) configuré(s)");}
    }catch(Exception e){v.error(e.getMessage());}}
    private record Finalized(Snapshot snapshot,String requestId){}
    private void finalizeForm(){try{
        v.showAction("formError",false);
        requirePermission("demand:edit");var data=formData();RequestValidation.forSubmission(data);String id=editing,context=parish();
        if(correcting!=null)throw new IllegalArgumentException("Enregistrez d'abord le brouillon de correction.");
        int confirmedAmount=v.plan.getValue().data().path("montantForfait").decimalValue().intValueExact();
        if(!confirm("Enregistrer cette demande pour "+ScreenFormats.money(v.plan.getValue().data().path("montantForfait"))+" ?"))return;
        run(()->{var request=new LocalRequestService(store).finalizePayload(id,data,confirmedAmount);return new Finalized(snapshot(context),request.path("localRequestId").asText());},result->{display(result.snapshot());reset();v.tabs.getSelectionModel().select(1);v.resetDemandFilters();for(var row:v.demands.getItems())if(result.requestId().equals(row.data().path("localRequestId").asText())){v.demands.getSelectionModel().select(row);v.demands.scrollTo(row);break;}v.status.setText("Demande enregistrée. Vous pouvez poursuivre son traitement.");});
    }catch(Exception failure){v.error(failure.getMessage());}}
    private void save(){saveDraft(null);}
    private void saveDraft(Runnable next){try{
        if(parishMode)requirePermission("demand:edit");
        parish();var data=formData();String id=editing,rejected=correcting,context=parish();
        run(()->{if(rejected!=null)store.correctDraft(rejected,data);else store.draft(id,data);return snapshot(context);},s->{display(s);reset();v.tabs.getSelectionModel().select(2);v.status.setText("Brouillon enregistré. Vous pourrez le compléter avant de le transmettre.");if(next!=null)next.run();});
    }catch(Exception e){v.error(e.getMessage());}}
    private JsonNode selected(TableView<Row> list){if(list.getSelectionModel().getSelectedItem()==null)throw new IllegalArgumentException("Sélectionnez une ligne.");return list.getSelectionModel().getSelectedItem().data();}
    private boolean confirm(String text){var dialog=v.decorate(new Alert(Alert.AlertType.CONFIRMATION,text,ButtonType.CANCEL,ButtonType.OK));dialog.setTitle("Confirmer votre action");dialog.setHeaderText(null);return dialog.showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK;}
    private void queue(){try{requirePermission("demand:edit");var selected=selected(v.outbox);RequestValidation.forSubmission(selected.path("payload"));String id=selected.path("id").asText(),parish=parish();JsonNode quote=json.missingNode();for(var candidate:catalog.path("forfaits").path(selected.path("payload").path("typeDemandePublicId").asText()))if(candidate.path("publicId").asText().equals(selected.path("payload").path("forfaitTarifPublicId").asText()))quote=candidate.path("montantForfait");if(!quote.isNumber())throw new IllegalArgumentException("Le forfait doit être actualisé avant la finalisation.");int confirmedAmount=quote.decimalValue().intValueExact();if(!confirm("Enregistrer cette demande pour "+ScreenFormats.money(quote)+" ?"))return;run(()->{new LocalRequestService(store).finalizeDraft(id,confirmedAmount);return snapshot(parish);},s->{display(s);v.status.setText("Demande enregistrée.");});}catch(Exception e){v.error(e.getMessage());}}
    private void returnToDraft(){try{
        var entry=selected(v.outbox);String id=entry.path("id").asText(),context=parish();
        if(!confirm("Remettre cette demande en brouillon avant son premier envoi ? Elle ne sera plus transmise automatiquement."))return;
        run(()->{store.returnToDraft(id);return snapshot(context);},s->{display(s);v.status.setText("Demande remise en brouillon. Vous pouvez la modifier avant de la transmettre.");});
    }catch(Exception e){v.error(e.getMessage());}}
    private void retry(){try{
        if(api==null)throw new IllegalArgumentException("Reconnectez-vous avant de retenter la transmission.");
        var selected=selected(v.outbox);String id=selected.path("id").asText(),context=parish();
        if(!confirm("Retenter la transmission de cette opération déjà enregistrée ? Le même identifiant sera conservé pour empêcher un doublon."))return;
        run(()->{syncService().verifySession();store.retryBlocked(id);return snapshot(context);},this::display);
    }catch(Exception e){v.error(e.getMessage());}}
    private void archive(){try{String id=selected(v.outbox).path("id").asText(),parish=parish();if(!confirm("Archiver ce brouillon ou rejet ?"))return;run(()->{store.archive(id);return snapshot(parish);},this::display);}catch(Exception e){v.error(e.getMessage());}}
    private void edit(){try{
        if(parishMode)requirePermission("demand:edit");
        var e=selected(v.outbox);String state=e.path("state").asText();
        if(!"CREATE_DEMANDE".equals(e.path("kind").asText())||!("DRAFT".equals(state)||"REJECTED".equals(state)))throw new IllegalArgumentException("Seuls les brouillons et demandes rejetées sont modifiables.");
        if(!parish().equals(e.path("payload").path("paroissePublicId").asText()))throw new IllegalArgumentException("Choisissez la paroisse correspondant à ce brouillon.");
        protectForm(()->loadDraft(e));
    }catch(Exception e){v.error(e.getMessage());}}
    private void loadDraft(JsonNode e){
        String state=e.path("state").asText();var p=e.path("payload");
        loading=true;editing="DRAFT".equals(state)?e.path("id").asText():null;correcting="REJECTED".equals(state)?e.path("id").asText():null;choose(v.type,p.path("typeDemandePublicId").asText());fillPlans();choose(v.plan,p.path("forfaitTarifPublicId").asText());choose(v.schedule,p.path("horairePublicId").asText());choose(v.payment,p.path("typePaiementPublicId").asText());v.surname.setText(p.path("nomFidele").asText());v.firstname.setText(p.path("prenomFidele").asText());v.phone.setText(p.path("telFidele").asText());v.email.setText(p.path("emailFidele").asText());v.courier.setText(p.path("nomCoursier").asText());v.intention.setText(p.path("intention").asText());v.date.setValue(p.path("dateDebut").asText().isBlank()?null:LocalDate.parse(p.path("dateDebut").asText()));if(p.has("_invalidDateText"))v.date.getEditor().setText(p.path("_invalidDateText").asText());v.customTime.setText(p.path("_customTimeText").asText(p.path("heurePersonnalisee").asText("")));var extra=new ArrayList<String>();for(var d:p.path("datesCelebration"))if(!d.asText().equals(p.path("dateDebut").asText()))extra.add(d.asText());v.extraDates.getItems().setAll(extra.stream().map(LocalDate::parse).toList());configuredSlots=p.path("celebrationSlots").isArray()?(ArrayNode)p.path("celebrationSlots").deepCopy():null;v.slotsSummary.setText(configuredSlots==null?"Horaire commun par défaut":configuredSlots.size()+" créneau(x) configuré(s)");v.tabs.getSelectionModel().select(0);loading=false;calendarChanged(false);cleanForm=formData();
    }
    private void choose(ComboBox<Choice> box,String id){box.setValue(null);for(var c:box.getItems())if(c.id().equals(id)){box.setValue(c);break;}}
    private void refresh(){if(api==null){if(reconnectPassword!=null)reconnect();return;}try{String parish=parish();run(()->{var sync=syncService();sync.catalogue(parish);sync.refresh(parish);return snapshot(parish);},s->{display(s);v.status.setText("Les données de votre paroisse sont à jour.");});}catch(Exception e){v.error(e.getMessage());}}
    private void synchronize(){
        if(api==null){if(reconnectPassword!=null)reconnect();return;}
        if(closed||busy||synchronizing||store==null)return;
        LocalStore expected=store;ApiClient client=api;String context=parish();String bound=parishMode?deployment.parishId():null;long epoch=authEpoch;
        synchronizing=true;controls();
        var task=new Task<String>(){protected String call()throws Exception{
            var sync=new SyncService(expected,client,bound,()->!closed&&!isCancelled()&&epoch==authEpoch);String result=sync.synchronize();
            if(!isCancelled()&&epoch==authEpoch){sync.catalogue(context);sync.refresh(context);}return result;
        }};syncTask=task;
        task.setOnSucceeded(event->{if(closed||store!=expected||epoch!=authEpoch)return;synchronizing=false;syncTask=null;transientFailures=0;automaticRetryAfter=0;if(settingsRefreshing){settingsRefreshing=false;dataMessage.setText("Les données de la paroisse sont à jour.");}controls();reloadView(taskGeneration);});
        task.setOnFailed(event->{
            if(closed||store!=expected||epoch!=authEpoch)return;synchronizing=false;syncTask=null;Throwable failure=task.getException();if(settingsRefreshing){settingsRefreshing=false;dataMessage.setText(OperationErrors.message(failure));}
            if(failure instanceof SyncService.SessionMismatchException||(failure instanceof ApiClient.ApiException rejected&&rejected.status()==401)){client.clear();api=null;clearReconnect();}
            transientFailures=Math.min(transientFailures+1,6);automaticRetryAfter=System.currentTimeMillis()+Math.min(300000L,30000L*(1L<<transientFailures));if(!DeviceCredentials.unavailable(failure))v.status.setText(OperationErrors.message(failure));controls();reloadView(taskGeneration);
        });reconnector.submit(task);
    }
    private void cash(){try{requirePermission("payment:manage");var demand=selected(v.demands);if("PAYE".equals(demand.path("statutPaiement").asText()))throw new IllegalArgumentException("Cette demande est déjà payée.");if(!confirm("Confirmez-vous avoir reçu "+demand.path("montant").asText()+" XOF en espèces pour "+demand.path("codeSuivie").asText()+" ?"))return;String parish=parish();run(()->{if(demand.path("publicId").asText().startsWith("local:"))new LocalRequestService(store).receiveCash(demand.path("localRequestId").asText());else store.queueCash(demand);return snapshot(parish);},s->{display(s);v.status.setText("Encaissement enregistré. Le reçu est disponible.");});}catch(Exception e){v.error(e.getMessage());}}
    private void recoverPassword(){
        if(closed||busy)return;var dialog=v.decorate(new TextInputDialog(v.username.getText().contains("@")?v.username.getText():""));dialog.setTitle("Mot de passe oublié");dialog.setHeaderText("Recevoir un lien de réinitialisation");dialog.setContentText("Adresse e-mail du compte");var answer=dialog.showAndWait();if(answer.isEmpty())return;String email=answer.get().strip();if(!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")){v.error("Indiquez une adresse e-mail valide.");return;}
        run(()->{var client=new ApiClient(apiBaseUrl);try{return client.post("auth/forgot-password",json.createObjectNode().put("email",email));}finally{client.clear();}},response->v.status.setText("Si un compte correspond à cette adresse, un lien de réinitialisation sera envoyé. Consultez votre messagerie."));
    }
    private void settings(){settings(null);}
    private void settings(String initialModule){
        if(session==null||store==null||v.parish.getValue()==null)return;
        var settingsChanged=new java.util.concurrent.atomic.AtomicBoolean();
        var dialog=v.decorate(new Dialog<Void>(),1040);dialog.setTitle("Paramètres");dialog.getDialogPane().setId("settingsDialog");dialog.setHeaderText("Votre espace paroissial");dialog.getDialogPane().getButtonTypes().add(new ButtonType("Fermer",ButtonBar.ButtonData.CANCEL_CLOSE));
        String role=ScreenFormats.role(session.path("user").path("role").asText(""));var identity=new Label((deployment==null?(v.parish.getValue()==null?"Votre paroisse":v.parish.getValue().label()):deployment.parishName())+"\n"+session.path("user").path("username").asText("")+(role.isBlank()?"":" · "+role));identity.setWrapText(true);
        var refresh=new Button("Actualiser les données maintenant");refresh.setOnAction(e->{settingsRefreshing=true;dataMessage.setText("Actualisation en cours…");if(api==null)reconnect();else synchronize();});var info=new Label("Les horaires, forfaits et demandes sont actualisés automatiquement lorsque le service est accessible. Votre saisie en cours est conservée.");info.setWrapText(true);
        var version=new Label("Missanye Desktop · version "+com.missanye.desktop.service.UpdateService.currentVersion());var check=new Button("Rechercher une mise à jour");check.setOnAction(e->checkUpdates(true));updateMessage.setWrapText(true);
        updateDownload.setVisible(latestRelease!=null);updateDownload.setManaged(latestRelease!=null);updateDownload.setOnAction(e->upgrade());
        dataMessage.setWrapText(true);
        var box=new javafx.scene.layout.VBox(14,identity,new Separator(),new Label("Données de la paroisse"),info,refresh,dataMessage,new Separator(),version,check,updateMessage,updateDownload);box.setPrefWidth(490);
        var service=new com.missanye.desktop.admin.ParishAdministration(api,store,v.parish.getValue().id(),session);var expected=store;long epoch=authEpoch;
        var administration=new com.missanye.desktop.admin.AdministrationPane(service,v,()->!closed&&store==expected&&authEpoch==epoch,()->{settingsChanged.set(true);dataMessage.setText("Paramètres enregistrés. Les référentiels seront actualisés à la fermeture.");},()->{clearReconnect();try{if(deviceCredentials!=null)deviceCredentials.revoke(apiBaseUrl,session.path("user").path("username").asText());}catch(Exception ignored){}if(api!=null)api.clear();api=null;dataMessage.setText("Mot de passe modifié. Déconnectez-vous puis reconnectez-vous avec le nouveau mot de passe.");v.status.setText(dataMessage.getText());Platform.runLater(dialog::close);},initialModule);
        var tabs=new TabPane(new Tab("Parcours et paramètres paroissiaux",administration),new Tab("Application et mises à jour",box));tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);dialog.getDialogPane().setContent(tabs);
        dialog.setOnCloseRequest(event->{if(!administration.mayClose())event.consume();});
        try{dialog.showAndWait();}finally{administration.close();box.getChildren().removeAll(updateMessage,updateDownload,dataMessage);if(settingsChanged.get()&&api!=null&&!busy)refresh();else controls();}

    }
    private void checkUpdates(boolean explicit){
        if((!explicit&&Boolean.getBoolean("missanye.disableAutomaticUpdateChecks"))||closed||checkingUpdates||(!explicit&&System.currentTimeMillis()-updateCheckedAt<3600000))return;
        checkingUpdates=true;updateCheckedAt=System.currentTimeMillis();if(explicit)updateMessage.setText("Vérification des versions publiées…");
        var task=new Task<java.util.Optional<com.missanye.desktop.service.UpdateService.Release>>(){protected java.util.Optional<com.missanye.desktop.service.UpdateService.Release> call()throws Exception{return new com.missanye.desktop.service.UpdateService().check();}};
        task.setOnSucceeded(e->{if(closed)return;checkingUpdates=false;latestRelease=task.getValue().orElse(null);updateMessage.setText(latestRelease==null?"Aucune version publiée plus récente n'est disponible.":"La version "+latestRelease.version()+" est disponible.");updateDownload.setVisible(latestRelease!=null);updateDownload.setManaged(latestRelease!=null);v.setUpdateAvailable(latestRelease!=null);});
        task.setOnFailed(e->{checkingUpdates=false;if(!closed&&explicit)updateMessage.setText("Impossible de vérifier les versions pour le moment. Vous pouvez continuer à travailler.");});updater.submit(task);
    }
    private void upgrade(){
        if(latestRelease==null||checkingUpdates||closed||busy)return;
        if(!System.getProperty("os.name","").startsWith("Windows")){if(host!=null)host.showDocument(latestRelease.page().toString());return;}
        protectForm(()->{
            var release=latestRelease;checkingUpdates=true;updateDownload.setDisable(true);updateMessage.setText("Téléchargement et vérification de la version "+release.version()+"… Vous pouvez continuer à travailler.");
            var task=new Task<java.nio.file.Path>(){protected java.nio.file.Path call()throws Exception{return new com.missanye.desktop.service.UpdateService().prepare(release,file.toAbsolutePath().getParent().resolve("updates"));}};
            task.setOnSucceeded(event->{checkingUpdates=false;updateDownload.setDisable(false);if(closed)return;
                updateMessage.setText("Mise à jour vérifiée. Prête à installer.");
                // Recheck pending work: the user may have edited while the download was running.
                requestClose(()->run(()->{com.missanye.desktop.service.UpdateService.install(task.getValue());return true;},installed->Platform.exit()));
            });
            task.setOnFailed(event->{checkingUpdates=false;updateDownload.setDisable(false);if(!closed)updateMessage.setText("La mise à jour n’a pas pu être téléchargée ou vérifiée. Réessayez. Votre version reste utilisable.");});updater.submit(task);
        });
    }
    private void copyDemand(String field){try{
        var demand=selected(v.demands);String value=switch(field){case "code"->ScreenFormats.reference(demand);case "phone"->demand.path("telFidele").asText("");default->ScreenFormats.details(demand);};
        if(value.isBlank())throw new IllegalArgumentException("Aucune information à copier.");var content=new javafx.scene.input.ClipboardContent();content.putString(value);javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);v.clearError();v.status.setText("Copié dans le presse-papiers.");
    }catch(Exception invalid){v.error(invalid.getMessage());}}
    private record SheetSelection(LocalDate date,boolean includeUnpaid,List<LocalTime> hours){}
    private void printIntention(){try{
        requirePermission("demand:read");String parish=parish();var dialog=v.decorate(new Dialog<SheetSelection>());dialog.setTitle("Feuille d’intentions");dialog.setHeaderText("Intentions du jour regroupées par heure de messe");dialog.getDialogPane().setId("celebrationSheetDialog");
        var date=new DatePicker(LocalDate.now(java.time.ZoneId.of("Africa/Lome")));var include=new CheckBox("Inclure les intentions validées non payées");var hours=new ListView<com.missanye.printing.CelebrationSheetPdf.Mass>();hours.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);hours.setPrefHeight(170);hours.setCellFactory(list->new ListCell<>(){@Override protected void updateItem(com.missanye.printing.CelebrationSheetPdf.Mass mass,boolean empty){super.updateItem(mass,empty);setText(empty||mass==null?null:mass.label()+" · "+mass.intentions().size()+" intention(s)");}});
        var summary=new Label();summary.setWrapText(true);var note=new Label("Sans sélection d’heure, toute la journée sera imprimée. Ctrl+clic permet de sélectionner plusieurs messes.");note.setWrapText(true);
        var print=new ButtonType("Préparer l’impression",ButtonBar.ButtonData.OK_DONE);dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL,print);
        Runnable refresh=()->{var rows=json.createArrayNode();for(var row:v.demandRows)rows.add(row.data());var groups=date.getValue()==null?List.<com.missanye.printing.CelebrationSheetPdf.Mass>of():com.missanye.desktop.service.IntentionSheet.groups(rows,catalog,date.getValue(),include.isSelected(),List.of());hours.getItems().setAll(groups);summary.setText(groups.stream().mapToInt(g->g.intentions().size()).sum()+" intention(s) · "+groups.size()+" messe(s)");dialog.getDialogPane().lookupButton(print).setDisable(groups.isEmpty());};
        date.valueProperty().addListener((o,old,value)->refresh.run());include.selectedProperty().addListener((o,old,value)->refresh.run());dialog.getDialogPane().setContent(new javafx.scene.layout.VBox(12,new Label("Date de célébration"),date,include,new Label("Messes à imprimer"),hours,note,summary));refresh.run();
        dialog.setResultConverter(button->button==print?new SheetSelection(date.getValue(),include.isSelected(),hours.getSelectionModel().getSelectedItems().stream().map(com.missanye.printing.CelebrationSheetPdf.Mass::time).filter(java.util.Objects::nonNull).toList()):null);var selection=dialog.showAndWait();if(selection.isEmpty())return;var chosen=selection.get();
        var chooser=new FileChooser();chooser.setTitle("Imprimer / exporter la feuille d’intentions");chooser.setInitialFileName("feuille-intentions-"+chosen.date()+".pdf");chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF","*.pdf"));var target=chooser.showSaveDialog(v.root.getScene().getWindow());if(target==null)return;
        run(()->{ReceiptExporter.write(target.toPath(),new LocalRequestService(store).celebrationSheet(parish,chosen.date(),chosen.includeUnpaid(),chosen.hours()));return true;},done->{if(host!=null)host.showDocument(target.toURI().toString());v.status.setText("Feuille d’intentions prête à imprimer dans votre lecteur PDF.");});
    }catch(Exception invalid){v.error(invalid.getMessage());}}
    private void receipt(){try{
        if(!hasPermission("invoice:read")&&!hasPermission("receipt:manage"))throw new IllegalArgumentException("Vous ne disposez pas des droits pour éditer un reçu.");
        var demand=selected(v.demands);String code=demand.path("localReference").asText(demand.path("codeSuivie").asText());if(code.isBlank())throw new IllegalArgumentException("Code de suivi absent");
        var chooser=new FileChooser();chooser.setTitle("Exporter le reçu");chooser.setInitialFileName("recu-"+code.replaceAll("[^A-Za-z0-9_-]","_")+".pdf");chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF","*.pdf"));var target=chooser.showSaveDialog(v.root.getScene().getWindow());if(target==null)return;
        run(()->{
            byte[] document=new LocalRequestService(store).receiptFor(demand);
            ReceiptExporter.write(target.toPath(),document);return true;
        },exported->{if(host!=null)host.showDocument(target.toURI().toString());v.status.setText("Reçu prêt à imprimer.");});
    }catch(Exception failure){v.error(failure.getMessage());}}
    private void checkout(){try{var d=selected(v.demands);if(api==null)throw new IllegalArgumentException("Une connexion est nécessaire pour effectuer un paiement en ligne.");String code=d.path("codeSuivie").asText();if(!confirm("Préparer le paiement en ligne de "+code+" ? Le prestataire affichera le montant avec ses frais."))return;run(()->api.post("paiements/checkout/"+java.net.URLEncoder.encode(code,java.nio.charset.StandardCharsets.UTF_8),json.createObjectNode()),result->{String url=result.path("paymentUrl").asText();if(!url.isBlank()){var uri=java.net.URI.create(url);if(!"https".equals(uri.getScheme()) || uri.getHost()==null || uri.getUserInfo()!=null){v.status.setText("Le lien de paiement ne peut pas être ouvert. Contactez votre administrateur.");return;}if(host!=null)host.showDocument(url);v.status.setText("Paiement ouvert dans le navigateur. Cliquez ensuite sur Vérifier le paiement.");}else v.status.setText(result.path("message").asText("Pas d'URL de paiement disponible."));});}catch(Exception e){v.error(e.getMessage());}}
    private void reconcile(){try{String code=selected(v.demands).path("codeSuivie").asText();run(()->api.post("paiements/reconcile/"+java.net.URLEncoder.encode(code,java.nio.charset.StandardCharsets.UTF_8),json.createObjectNode()),r->{v.status.setText("État vérifié : "+ScreenFormats.state(r.path("statutPaiement").asText()));refresh();});}catch(Exception e){v.error(e.getMessage());}}
    private void clearReconnect(){if(reconnectPassword!=null)Arrays.fill(reconnectPassword,'\0');reconnectPassword=null;reconnectUsername=null;}
    private void cancelReconnect(){authEpoch++;if(syncTask!=null)syncTask.cancel(true);syncTask=null;synchronizing=false;if(reconnectTask!=null)reconnectTask.cancel(true);reconnectTask=null;reconnecting=false;}
    private void reconnect(){
        if(closed||busy||reconnecting||synchronizing||store==null||deployment==null||reconnectPassword==null)return;
        String username=reconnectUsername;char[] secret=reconnectPassword.clone();LocalStore expected=store;var selected=deployment;long epoch=authEpoch;
        reconnecting=true;controls();
        var task=new Task<ParishLoginService.Reconnected>(){protected ParishLoginService.Reconnected call()throws Exception{
            var result=ParishLoginService.authenticateReconnect(expected.owner(),selected,deviceCredentials,username,secret);if(isCancelled()||epoch!=authEpoch){result.api().clear();throw new InterruptedException();}return result;
        }};reconnectTask=task;
        task.setOnSucceeded(event->{
            var result=task.getValue();if(closed||store!=expected||epoch!=authEpoch){result.api().clear();return;}
            // Only the final local commit joins the serial I/O queue. Network waiting never disables the form.
            worker.submit(()->{try{
                if(closed||store!=expected||epoch!=authEpoch){result.api().clear();return;}
                expected.saveSession(result.session(),result.parishes());deviceCredentials.save(selected.apiUrl(),username,result.ticket());
                Platform.runLater(()->{if(closed||store!=expected||epoch!=authEpoch){result.api().clear();return;}api=result.api();session=result.session();reconnecting=false;reconnectTask=null;clearReconnect();automaticRetryAfter=0;controls();if(!busy){checkUpdates(false);synchronize();}});
            }catch(Exception failure){result.api().clear();Platform.runLater(()->reconnectFailure(expected,epoch,failure));}});
        });
        task.setOnFailed(event->reconnectFailure(expected,epoch,task.getException()));reconnector.submit(task);
    }
    private void reconnectFailure(LocalStore expected,long epoch,Throwable failure){
        if(closed||store!=expected||epoch!=authEpoch)return;reconnecting=false;reconnectTask=null;if(settingsRefreshing){settingsRefreshing=false;dataMessage.setText(OperationErrors.message(failure));}
        if(failure instanceof ParishLoginService.ReauthenticationRequired||failure instanceof SyncService.SessionMismatchException)clearReconnect();
        transientFailures=Math.min(transientFailures+1,6);automaticRetryAfter=System.currentTimeMillis()+Math.min(300000L,30000L*(1L<<transientFailures));
        if(!DeviceCredentials.unavailable(failure))v.status.setText(OperationErrors.message(failure));controls();
    }
    @Override public void close(){closed=true;cancelReconnect();clearReconnect();reconnector.shutdownNow();updater.shutdownNow();readinessDelay.stop();timer.shutdownNow();worker.shutdownNow();if(api!=null)api.clear();if(!busy&&store!=null)store.close();}
}
