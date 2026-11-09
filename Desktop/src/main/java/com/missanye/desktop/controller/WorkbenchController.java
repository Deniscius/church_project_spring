package com.missanye.desktop.controller;

import com.missanye.desktop.api.ApiClient;
import com.missanye.desktop.config.DesktopConfiguration;
import com.missanye.desktop.model.ScreenFormats;
import com.missanye.desktop.model.RequestValidation;
import com.missanye.desktop.service.ReceiptExporter;
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
    private final String apiBaseUrl;
    private final HostServices host;
    private final ObjectMapper json=new ObjectMapper();
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"desktop-io");t.setDaemon(true);return t;});
    private final ScheduledExecutorService timer=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"desktop-sync-timer");t.setDaemon(true);return t;});
    private LocalStore store;
    private ApiClient api;
    private JsonNode session,catalog;
    private String editing,correcting;
    private Choice currentParish;
    private ObjectNode cleanForm;
    private volatile boolean closed;
    private long taskGeneration;
    private boolean offlineReady;
    private final javafx.animation.PauseTransition readinessDelay=new javafx.animation.PauseTransition(javafx.util.Duration.millis(220));
    private ArrayNode configuredSlots;
    private boolean busy,loading;
    private int transientFailures;
    private long automaticRetryAfter;
    public WorkbenchController(WorkbenchView view,Path file,HostServices host){
        this(view,file,host,DesktopConfiguration.load().apiBaseUrl());
    }
    public WorkbenchController(WorkbenchView view,Path file,HostServices host,String configuredApiUrl){
        apiBaseUrl=new DesktopConfiguration(configuredApiUrl).apiBaseUrl();
        v=view;this.file=file;this.host=host;
        v.login.setOnAction(e->unlock(true));v.offline.setOnAction(e->unlock(false));v.lock.setOnAction(e->protectForm(this::lock));
        v.parish.setOnAction(e->{if(!loading && !busy)changeParish();});
        v.addDate.setOnAction(e->addDate());v.removeDate.setOnAction(e->{var chosen=v.extraDates.getSelectionModel().getSelectedItem();if(chosen!=null)v.extraDates.getItems().remove(chosen);});
        v.configureSlots.setOnAction(e->configureSlots());
        v.type.setOnAction(e->{if(!loading)fillPlans();});v.plan.setOnAction(e->price());
        v.fresh.setOnAction(e->protectForm(this::reset));v.save.setOnAction(e->save());v.queue.setOnAction(e->queue());v.edit.setOnAction(e->edit());v.archive.setOnAction(e->archive());v.retry.setOnAction(e->retry());v.returnToDraft.setOnAction(e->returnToDraft());
        v.correct.setOnAction(e->mutate("UPDATE_INTENTION"));v.validate.setOnAction(e->mutate("VALIDATE_DEMANDE"));v.remove.setOnAction(e->mutate("DELETE_DEMANDE"));
        v.refresh.setOnAction(e->refresh());v.sync.setOnAction(e->synchronize());v.cash.setOnAction(e->cash());v.receipt.setOnAction(e->receipt());v.checkout.setOnAction(e->checkout());v.reconcile.setOnAction(e->reconcile());
        v.demands.getSelectionModel().selectedItemProperty().addListener((o,a,b)->{v.detail.setText(b==null?"Sélectionnez une demande pour consulter son intention et agir.":description(b.data()));updateActions();});
        v.outbox.getSelectionModel().selectedItemProperty().addListener((o,a,b)->{v.queueDetail.setText(b==null?"Sélectionnez une opération pour consulter son état.":b.data().path("error").asText("Cette opération est conservée sur votre poste. Les confirmations apparaissent après transmission."));updateActions();});
        cleanForm=formData();
        readinessDelay.setOnFinished(e->inspectOfflineReadiness());v.username.textProperty().addListener((o,a,b)->{offlineReady=false;controls();readinessDelay.playFromStart();});
        controls();
        timer.scheduleWithFixedDelay(()->Platform.runLater(()->{if(!closed && !busy && api!=null && store!=null && v.auto.isSelected() && !v.hasOpenDialog() && System.currentTimeMillis()>=automaticRetryAfter)synchronize();}),60,60,TimeUnit.SECONDS);
    }
    private <T> void run(Callable<T> action,Consumer<T> success){
        if(busy||closed)return;busy=true;long generation=++taskGeneration;controls();
        var task=new Task<T>(){@Override protected T call()throws Exception{try{return action.call();}finally{if(closed&&store!=null)store.close();}}};
        task.setOnSucceeded(e->{if(closed)return;busy=false;controls();try{success.accept(task.getValue());}catch(Exception x){v.status.setText("Affichage impossible : "+x.getMessage());}});
        task.setOnFailed(e->{if(closed)return;busy=false;
            if(task.getException() instanceof ApiClient.ApiException a && (a.status()==401)){if(api!=null)api.clear();api=null;v.session.setText("Votre session a expiré ou vos droits ont changé. Déconnectez-vous puis reconnectez-vous pour transmettre votre travail.");}
            Throwable x=task.getException();if(x instanceof SyncService.SessionMismatchException){if(api!=null)api.clear();api=null;}
            if(store==null)v.showLogin();controls();
            if(x instanceof java.io.IOException || (x instanceof ApiClient.ApiException a && (a.status()==429 || a.status()>=500))) {transientFailures=Math.min(transientFailures+1,6);automaticRetryAfter=System.currentTimeMillis()+Math.min(3600000L,60000L*(1L<<transientFailures));}
            v.status.setText(OperationErrors.message(x));
            reloadAfterFailure(generation);
        });worker.submit(task);
    }
    private void controls(){
        boolean opened=store!=null;v.setWorkspaceOpen(opened);v.workspace.setDisable(busy||!opened);v.lock.setDisable(busy||!opened);
        v.busyIndicator.setVisible(busy);v.busyIndicator.setManaged(busy);
        for(var n:List.of(v.username,v.password,v.localPassword,v.login,v.offline))n.setDisable(busy||opened);
        v.offlinePane.setDisable(busy||opened||!offlineReady);v.offline.setDisable(busy||opened||!offlineReady);
        v.refresh.setDisable(api==null);v.sync.setDisable(api==null);updateActions();
    }
    private String parish(){if(v.parish.getValue()==null)throw new IllegalArgumentException("Sélectionnez une paroisse.");return v.parish.getValue().id();}
    private void inspectOfflineReadiness(){
        if(closed||store!=null)return;String username=v.username.getText().trim();
        if(username.isBlank()){offlineReady=false;v.offlineAvailability.setText("Une connexion Internet est nécessaire lors de la première utilisation sur ce poste.");controls();return;}
        worker.submit(()->{
            LocalStore.ProfileStatus status;
            try{status=LocalStore.inspect(file,apiBaseUrl,username);}catch(Exception e){status=LocalStore.ProfileStatus.NEEDS_ONLINE;}
            final var result=status;Platform.runLater(()->{if(closed||store!=null||!username.equals(v.username.getText().trim()))return;offlineReady=result==LocalStore.ProfileStatus.READY;
                v.offlineAvailability.setText(offlineReady?"Ce compte dispose d'un espace préparé sur ce poste.":result==LocalStore.ProfileStatus.ABSENT?"Première utilisation : connectez-vous en ligne pour préparer ce poste.":"La préparation de ce compte doit être terminée en ligne.");controls();});
        });
    }
    private record Authenticated(ApiClient api,JsonNode session,JsonNode parishes,boolean firstProfile,String username){}
    private record Opened(ApiClient api,LocalStore store,JsonNode session,JsonNode parishes,String selected,boolean preparedNow){}
    private void unlock(boolean online){
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
                if(parishes.isEmpty())throw new IllegalArgumentException("Aucune paroisse active n'est accessible avec ce compte. Contactez votre administrateur.");
                return new Authenticated(client,info,parishes,LocalStore.inspect(file,apiBaseUrl,username)==LocalStore.ProfileStatus.ABSENT,username);
            }catch(Exception e){client.clear();throw e;}
        },authenticated->{
            try{
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
        loading=false;v.session.setText((api==null?"Hors ligne — ":"En ligne — ")+session.path("user").path("username").asText(v.username.getText()));controls();
        v.status.setText(opened.preparedNow()?"Votre espace est prêt. L'accès hors connexion est activé pour cette paroisse.":api==null?"Votre espace est ouvert hors connexion. Les opérations seront transmises après reconnexion.":"Connexion réussie. Vos données sont disponibles.");
        openParish(v.parish.getValue(),!opened.preparedNow());
    }
    private void lock(){
        var client=api;run(()->{try{if(client!=null)client.logout();}catch(Exception ignored){}finally{if(store!=null)store.close();}return true;},ignored->{store=null;api=null;session=null;catalog=null;editing=null;currentParish=null;v.parish.getItems().clear();v.demandRows.clear();v.activityRows.clear();v.demandSearch.clear();v.queueFilter.getSelectionModel().selectFirst();v.type.getItems().clear();v.plan.getItems().clear();v.schedule.getItems().clear();v.payment.getItems().clear();reset();v.detail.setText("");v.queueDetail.setText("");v.session.setText("Coffre verrouillé");v.status.setText("Vous êtes déconnecté. Votre travail reste protégé sur ce poste.");controls();inspectOfflineReadiness();});
    }
    private record Snapshot(JsonNode catalogue,JsonNode demands,List<ObjectNode> entries,JsonNode session){}
    private Snapshot snapshot(String parish)throws Exception{return new Snapshot(store.get("catalogue:"+parish),store.get("demands:"+parish),store.entries(),store.get("session"));}
    private void reloadAfterFailure(long generation){
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
            fillPlans();if(dirty||!oldPlan.isBlank())choose(v.plan,oldPlan);loading=false;price();
        }
        if(s.session().path("user").path("id").asText().equals(store.owner()))session=s.session();
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
        boolean ready=store!=null&&!busy;var demand=v.demands.getSelectionModel().getSelectedItem();var activity=v.outbox.getSelectionModel().getSelectedItem();
        boolean paid=demand!=null&&"PAYE".equals(demand.data().path("statutPaiement").asText());
        v.correct.setDisable(!ready||demand==null||!hasPermission("demand:edit"));
        v.validate.setDisable(!ready||demand==null||!hasPermission("demand:validate")||!demand.data().path("validationRequise").asBoolean());
        v.remove.setDisable(!ready||demand==null||paid||!hasPermission("demand:delete"));
        boolean waiting=demand!=null&&v.activityRows.stream().anyMatch(row->Set.of("PENDING","BLOCKED").contains(row.data().path("state").asText())&&demand.data().path("publicId").asText().equals(row.data().path("payload").path("demandePublicId").asText()));
        v.cash.setDisable(!ready||demand==null||paid||waiting||!hasPermission("payment:manage")||(demand.data().has("paiementDisponible")&&!demand.data().path("paiementDisponible").asBoolean()));
        v.receipt.setDisable(!ready||demand==null);v.checkout.setDisable(!ready||api==null||demand==null||paid);v.reconcile.setDisable(!ready||api==null||demand==null);
        String state=activity==null?"":activity.data().path("state").asText();boolean editable=activity!=null&&"CREATE_DEMANDE".equals(activity.data().path("kind").asText())&&(state.equals("DRAFT")||state.equals("REJECTED"));
        v.queue.setDisable(!ready||!state.equals("DRAFT")||!hasPermission("demand:edit"));v.edit.setDisable(!ready||!editable);v.archive.setDisable(!ready||!(state.equals("DRAFT")||state.equals("REJECTED")));v.retry.setDisable(!ready||api==null||!state.equals("BLOCKED"));v.returnToDraft.setDisable(!ready||activity==null||!state.equals("PENDING")||!"CREATE_DEMANDE".equals(activity.data().path("kind").asText())||activity.data().path("attempts").asInt()!=0);
    }
    private String stateLabel(String state){return ScreenFormats.state(state);}
    private String kindLabel(String kind){return switch(kind){case "CREATE_DEMANDE"->"Nouvelle demande";case "CASH_PAYMENT"->"Encaissement espèces";case "UPDATE_INTENTION"->"Correction d'intention";case "VALIDATE_DEMANDE"->"Décision";case "DELETE_DEMANDE"->"Suppression";default->kind;};}
    private void mutate(String kind){try{
        requirePermission(switch(kind){case "UPDATE_INTENTION"->"demand:edit";case "VALIDATE_DEMANDE"->"demand:validate";default->"demand:delete";});
        var d=selected(v.demands);String text=null,decision=null;
        if("UPDATE_INTENTION".equals(kind)){var dialog=v.decorate(new TextInputDialog(d.path("intention").asText()));dialog.setTitle("Corriger l'intention");dialog.setHeaderText("Correction envoyée à la prochaine synchronisation");var value=dialog.showAndWait();if(value.isEmpty())return;text=value.get().trim();if(text.length()<10 || text.length()>500)throw new IllegalArgumentException("L'intention doit contenir 10 à 500 caractères.");}
        else if("VALIDATE_DEMANDE".equals(kind)){var dialog=v.decorate(new ChoiceDialog<>("Valider",List.of("Valider","Rejeter")));dialog.setTitle("Décision sur la demande");dialog.setHeaderText(d.path("codeSuivie").asText());var value=dialog.showAndWait();if(value.isEmpty())return;decision="Valider".equals(value.get())?"VALIDEE":"REJETEE";}
        if(!confirm("Préparer cette opération pour "+d.path("codeSuivie").asText()+" ? Une vérification protégera les éventuelles modifications effectuées entre-temps."))return;
        String intention=text,statut=decision,parish=parish();run(()->{store.queueMutation(kind,d,intention,statut);return snapshot(parish);},s->{display(s);v.tabs.getSelectionModel().select(2);v.status.setText("Opération préparée. Elle sera confirmée après transmission.");});
    }catch(Exception e){v.status.setText(e.getMessage());}}
    private String description(JsonNode d){return d.path("codeSuivie").asText()+" · "+ScreenFormats.faithful(d)+" · "+ScreenFormats.money(d.path("montant"))+" · "+ScreenFormats.state(d.path("statutPaiement").asText())+"\n"+d.path("intention").asText();}
    private void setChoices(ComboBox<Choice> box,JsonNode values,String label){box.getItems().clear();for(var value:values)if(value.path("isActive").asBoolean(true) && !value.path("statusDel").asBoolean()){String text=value.path(label).asText(value.path("publicId").asText());if(value.has("heureCelebration"))text+=" — "+value.path("jourSemaine").asText()+" "+value.path("heureCelebration").asText();box.getItems().add(new Choice(value.path("publicId").asText(),text,value));}if(!box.getItems().isEmpty())box.getSelectionModel().selectFirst();}
    private void fillPlans(){if(catalog==null || v.type.getValue()==null){v.plan.getItems().clear();return;}setChoices(v.plan,catalog.path("forfaits").path(v.type.getValue().id()),"nomForfait");price();}
    private void requirePermission(String permission){
        if(session!=null)for(var p:session.path("user").path("permissions"))if(permission.equals(p.asText()))return;
        throw new IllegalArgumentException("Vous ne disposez pas des droits nécessaires pour cette action.");
    }
    private void price(){v.price.setText(v.plan.getValue()==null?"Aucun forfait disponible":"Montant indicatif : "+ScreenFormats.money(v.plan.getValue().data().path("montantForfait"))+" — confirmé lors de la transmission.");}
    private void reset(){editing=null;correcting=null;configuredSlots=null;v.slotsSummary.setText("Horaire commun par défaut");v.surname.clear();v.firstname.clear();v.phone.clear();v.email.clear();v.courier.clear();v.intention.clear();v.extraDates.getItems().clear();v.customTime.clear();v.date.setValue(LocalDate.now().plusDays(2));v.date.getEditor().setText(v.date.getConverter().toString(v.date.getValue()));v.additionalDate.setValue(v.date.getValue().plusDays(1));cleanForm=formData();}
    private String required(ComboBox<Choice> box,String label){if(box.getValue()==null)throw new IllegalArgumentException(label+" obligatoire.");return box.getValue().id();}
    private String id(ComboBox<Choice> choice){return choice.getValue()==null?"":choice.getValue().id();}
    private ObjectNode formData(){
        var p=json.createObjectNode().put("paroissePublicId",currentParish==null?"":currentParish.id()).put("typeDemandePublicId",id(v.type)).put("forfaitTarifPublicId",id(v.plan)).put("typePaiementPublicId",id(v.payment)).put("horairePublicId",id(v.schedule));
        LocalDate first=null;String typedDate=v.date.getEditor().getText();
        try{if(typedDate!=null&&!typedDate.isBlank())first=v.date.getConverter().fromString(typedDate);}catch(Exception invalid){p.put("_invalidDateText",typedDate);}
        p.put("nomFidele",v.surname.getText()).put("prenomFidele",v.firstname.getText()).put("telFidele",v.phone.getText().trim()).put("emailFidele",v.email.getText().trim()).put("nomCoursier",v.courier.getText()).put("intention",v.intention.getText()).put("dateDebut",first==null?"":first.toString()).put("_customTimeText",v.customTime.getText());
        p.put("_expectedCelebrations",v.plan.getValue()==null?0:v.plan.getValue().data().path("nombreCelebration").asInt(1));
        var dates=new TreeSet<LocalDate>();if(first!=null)dates.add(first);dates.addAll(v.extraDates.getItems());var list=p.putArray("datesCelebration");dates.forEach(d->list.add(d.toString()));
        if(configuredSlots!=null)p.set("celebrationSlots",configuredSlots.deepCopy());return p;
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
    private void configureSlots(){try{
        if(v.date.getValue()==null)throw new IllegalArgumentException("Date obligatoire");
        var dates=new TreeSet<LocalDate>();dates.add(v.date.getValue());dates.addAll(v.extraDates.getItems());
        if(dates.size()>30)throw new IllegalArgumentException("30 dates au maximum.");
        var dialog=v.decorate(new Dialog<ArrayNode>());dialog.setTitle("Créneaux de célébration");dialog.setHeaderText("Choisir l'horaire ou l'heure personnalisée pour chaque date. Le serveur vérifiera leur disponibilité.");dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL,ButtonType.OK);
        var grid=new javafx.scene.layout.GridPane();grid.setHgap(8);grid.setVgap(8);var choices=new ArrayList<ComboBox<Choice>>();var hours=new ArrayList<TextField>();int row=0;
        for(var date:dates){var box=new ComboBox<Choice>();box.getItems().setAll(v.schedule.getItems());box.setValue(v.schedule.getValue());var time=new TextField(v.customTime.getText());time.setPromptText("Heure personnalisée HH:mm");grid.addRow(row++,new Label(date.toString()),box,time);choices.add(box);hours.add(time);}
        dialog.getDialogPane().setContent(new ScrollPane(grid));dialog.setResultConverter(button->{if(button!=ButtonType.OK)return null;try{var slots=json.createArrayNode();int i=0;for(var date:dates){var slot=slots.addObject().put("date",date.toString());String time=hours.get(i).getText().trim();if(!time.isBlank())slot.put("heurePersonnalisee",parseTime(time).toString());else slot.put("horairePublicId",required(choices.get(i),"Horaire du "+date));i++;}return slots;}catch(Exception invalid){v.status.setText("Créneau invalide : "+invalid.getMessage());return null;}});
        var result=dialog.showAndWait();if(result.isPresent()){configuredSlots=result.get();v.slotsSummary.setText(configuredSlots.size()+" créneau(x) configuré(s)");}
    }catch(Exception e){v.status.setText(e.getMessage());}}
    private void save(){saveDraft(null);}
    private void saveDraft(Runnable next){try{
        parish();var data=formData();String id=editing,rejected=correcting,context=parish();
        run(()->{if(rejected!=null)store.correctDraft(rejected,data);else store.draft(id,data);return snapshot(context);},s->{display(s);reset();v.tabs.getSelectionModel().select(2);v.status.setText("Brouillon enregistré. Vous pourrez le compléter avant de le transmettre.");if(next!=null)next.run();});
    }catch(Exception e){v.status.setText(e.getMessage());}}
    private JsonNode selected(TableView<Row> list){if(list.getSelectionModel().getSelectedItem()==null)throw new IllegalArgumentException("Sélectionnez une ligne.");return list.getSelectionModel().getSelectedItem().data();}
    private boolean confirm(String text){var dialog=v.decorate(new Alert(Alert.AlertType.CONFIRMATION,text,ButtonType.CANCEL,ButtonType.OK));dialog.setTitle("Confirmer votre action");dialog.setHeaderText(null);return dialog.showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK;}
    private void queue(){try{requirePermission("demand:edit");var selected=selected(v.outbox);RequestValidation.forSubmission(selected.path("payload"));String id=selected.path("id").asText(),parish=parish();if(!confirm("Transmettre cette demande à la paroisse ? Les dates et le montant seront vérifiés avant confirmation."))return;run(()->{store.queue(id);return snapshot(parish);},s->{display(s);v.status.setText("Demande prête à être transmise.");});}catch(Exception e){v.status.setText(e.getMessage());}}
    private void returnToDraft(){try{
        var entry=selected(v.outbox);String id=entry.path("id").asText(),context=parish();
        if(!confirm("Remettre cette demande en brouillon avant son premier envoi ? Elle ne sera plus transmise automatiquement."))return;
        run(()->{store.returnToDraft(id);return snapshot(context);},s->{display(s);v.status.setText("Demande remise en brouillon. Vous pouvez la modifier avant de la transmettre.");});
    }catch(Exception e){v.status.setText(e.getMessage());}}
    private void retry(){try{
        if(api==null)throw new IllegalArgumentException("Reconnectez-vous avant de retenter la transmission.");
        var selected=selected(v.outbox);String id=selected.path("id").asText(),context=parish();
        if(!confirm("Retenter la transmission de cette opération déjà enregistrée ? Le même identifiant sera conservé pour empêcher un doublon."))return;
        run(()->{new SyncService(store,api).verifySession();store.retryBlocked(id);return snapshot(context);},this::display);
    }catch(Exception e){v.status.setText(e.getMessage());}}
    private void archive(){try{String id=selected(v.outbox).path("id").asText(),parish=parish();if(!confirm("Archiver ce brouillon ou rejet ?"))return;run(()->{store.archive(id);return snapshot(parish);},this::display);}catch(Exception e){v.status.setText(e.getMessage());}}
    private void edit(){try{
        var e=selected(v.outbox);String state=e.path("state").asText();
        if(!"CREATE_DEMANDE".equals(e.path("kind").asText())||!("DRAFT".equals(state)||"REJECTED".equals(state)))throw new IllegalArgumentException("Seuls les brouillons et demandes rejetées sont modifiables.");
        if(!parish().equals(e.path("payload").path("paroissePublicId").asText()))throw new IllegalArgumentException("Choisissez la paroisse correspondant à ce brouillon.");
        protectForm(()->loadDraft(e));
    }catch(Exception e){v.status.setText(e.getMessage());}}
    private void loadDraft(JsonNode e){
        String state=e.path("state").asText();var p=e.path("payload");
        editing="DRAFT".equals(state)?e.path("id").asText():null;correcting="REJECTED".equals(state)?e.path("id").asText():null;choose(v.type,p.path("typeDemandePublicId").asText());fillPlans();choose(v.plan,p.path("forfaitTarifPublicId").asText());choose(v.schedule,p.path("horairePublicId").asText());choose(v.payment,p.path("typePaiementPublicId").asText());v.surname.setText(p.path("nomFidele").asText());v.firstname.setText(p.path("prenomFidele").asText());v.phone.setText(p.path("telFidele").asText());v.email.setText(p.path("emailFidele").asText());v.courier.setText(p.path("nomCoursier").asText());v.intention.setText(p.path("intention").asText());v.date.setValue(p.path("dateDebut").asText().isBlank()?null:LocalDate.parse(p.path("dateDebut").asText()));if(p.has("_invalidDateText"))v.date.getEditor().setText(p.path("_invalidDateText").asText());v.customTime.setText(p.path("_customTimeText").asText(p.path("heurePersonnalisee").asText("")));var extra=new ArrayList<String>();for(var d:p.path("datesCelebration"))if(!d.asText().equals(p.path("dateDebut").asText()))extra.add(d.asText());v.extraDates.getItems().setAll(extra.stream().map(LocalDate::parse).toList());configuredSlots=p.path("celebrationSlots").isArray()?(ArrayNode)p.path("celebrationSlots").deepCopy():null;v.slotsSummary.setText(configuredSlots==null?"Horaire commun par défaut":configuredSlots.size()+" créneau(x) configuré(s)");v.tabs.getSelectionModel().select(0);cleanForm=formData();
    }
    private void choose(ComboBox<Choice> box,String id){box.setValue(null);for(var c:box.getItems())if(c.id().equals(id)){box.setValue(c);break;}}
    private void refresh(){if(api==null)return;try{String parish=parish();run(()->{var sync=new SyncService(store,api);sync.catalogue(parish);sync.refresh(parish);return snapshot(parish);},s->{display(s);v.status.setText("Les données de votre paroisse sont à jour.");});}catch(Exception e){v.status.setText(e.getMessage());}}
    private void synchronize(){if(api==null)return;String parish=v.parish.getValue()==null?null:parish();run(()->{var sync=new SyncService(store,api);String result=sync.synchronize();transientFailures=0;automaticRetryAfter=0;if(parish!=null)sync.refresh(parish);return result;},result->{v.status.setText(result);if(parish!=null)run(()->snapshot(parish),this::display);});}
    private void cash(){try{requirePermission("payment:manage");var demand=selected(v.demands);if("PAYE".equals(demand.path("statutPaiement").asText()))throw new IllegalArgumentException("Cette demande est déjà payée.");if(!confirm("Confirmez-vous avoir reçu "+demand.path("montant").asText()+" XOF en espèces pour "+demand.path("codeSuivie").asText()+" ? L'enregistrement reste provisoire jusqu'à validation serveur."))return;String parish=parish();run(()->{store.queueCash(demand);return snapshot(parish);},s->{display(s);v.tabs.getSelectionModel().select(2);v.status.setText("Encaissement enregistré sur ce poste. En attente de confirmation après transmission.");});}catch(Exception e){v.status.setText(e.getMessage());}}
    private void receipt(){try{var demand=selected(v.demands);String code=demand.path("codeSuivie").asText();if(code.isBlank())throw new IllegalArgumentException("Code de suivi absent");var chooser=new FileChooser();chooser.setTitle("Exporter le reçu (fichier non chiffré)");chooser.setInitialFileName("recu-"+code.replaceAll("[^A-Za-z0-9_-]","_")+".pdf");chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF","*.pdf"));var target=chooser.showSaveDialog(v.root.getScene().getWindow());if(target==null)return;boolean online=api!=null;run(()->{var document=new SyncService(store,api).receiptDownload(code,online);ReceiptExporter.write(target.toPath(),document.bytes());return document.cached();},cached->{if(host!=null)host.showDocument(target.toURI().toString());v.status.setText(cached?"Copie du reçu conservée sur ce poste. Connectez-vous pour obtenir sa version la plus récente.":"Reçu téléchargé. Vous pouvez l’imprimer depuis votre lecteur PDF.");});}catch(Exception e){v.status.setText(e.getMessage());}}
    private void checkout(){try{var d=selected(v.demands);if(api==null)throw new IllegalArgumentException("Une connexion est nécessaire pour effectuer un paiement en ligne.");String code=d.path("codeSuivie").asText();if(!confirm("Préparer le paiement en ligne de "+code+" ? Le prestataire affichera le montant avec ses frais."))return;run(()->api.post("paiements/checkout/"+java.net.URLEncoder.encode(code,java.nio.charset.StandardCharsets.UTF_8),json.createObjectNode()),result->{String url=result.path("paymentUrl").asText();if(!url.isBlank()){var uri=java.net.URI.create(url);if(!"https".equals(uri.getScheme()) || uri.getHost()==null || uri.getUserInfo()!=null){v.status.setText("Le lien de paiement ne peut pas être ouvert. Contactez votre administrateur.");return;}if(host!=null)host.showDocument(url);v.status.setText("Paiement ouvert dans le navigateur. Cliquez ensuite sur Vérifier le paiement.");}else v.status.setText(result.path("message").asText("Pas d'URL de paiement disponible."));});}catch(Exception e){v.status.setText(e.getMessage());}}
    private void reconcile(){try{String code=selected(v.demands).path("codeSuivie").asText();run(()->api.post("paiements/reconcile/"+java.net.URLEncoder.encode(code,java.nio.charset.StandardCharsets.UTF_8),json.createObjectNode()),r->{v.status.setText("État vérifié : "+ScreenFormats.state(r.path("statutPaiement").asText()));refresh();});}catch(Exception e){v.status.setText(e.getMessage());}}
    @Override public void close(){closed=true;readinessDelay.stop();timer.shutdownNow();worker.shutdownNow();if(api!=null)api.clear();if(!busy&&store!=null)store.close();}
}
