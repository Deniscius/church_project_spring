package com.missanye.desktop.controller;

import com.missanye.desktop.api.ApiClient;
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
    private final HostServices host;
    private final ObjectMapper json=new ObjectMapper();
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"desktop-io");t.setDaemon(true);return t;});
    private final ScheduledExecutorService timer=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"desktop-sync-timer");t.setDaemon(true);return t;});
    private LocalStore store;
    private ApiClient api;
    private JsonNode session,catalog;
    private String editing;
    private ArrayNode configuredSlots;
    private boolean busy,loading;
    private int transientFailures;
    private long automaticRetryAfter;
    public WorkbenchController(WorkbenchView view,Path file,HostServices host){
        v=view;this.file=file;this.host=host;
        v.login.setOnAction(e->unlock(true));v.offline.setOnAction(e->unlock(false));v.lock.setOnAction(e->lock());
        v.parish.setOnAction(e->{if(!loading && !busy)changeParish();});
        v.configureSlots.setOnAction(e->configureSlots());
        v.type.setOnAction(e->{if(!loading)fillPlans();});v.plan.setOnAction(e->price());
        v.fresh.setOnAction(e->reset());v.save.setOnAction(e->save());v.queue.setOnAction(e->queue());v.edit.setOnAction(e->edit());v.archive.setOnAction(e->archive());
        v.correct.setOnAction(e->mutate("UPDATE_INTENTION"));v.validate.setOnAction(e->mutate("VALIDATE_DEMANDE"));v.remove.setOnAction(e->mutate("DELETE_DEMANDE"));
        v.refresh.setOnAction(e->refresh());v.sync.setOnAction(e->synchronize());v.cash.setOnAction(e->cash());v.receipt.setOnAction(e->receipt());v.checkout.setOnAction(e->checkout());v.reconcile.setOnAction(e->reconcile());
        v.demands.getSelectionModel().selectedItemProperty().addListener((o,a,b)->v.detail.setText(b==null?"":description(b.data())));
        v.outbox.getSelectionModel().selectedItemProperty().addListener((o,a,b)->v.queueDetail.setText(b==null?"":b.data().path("error").asText("Les opérations en attente sont immuables. Corriger un rejet crée une nouvelle opération.")));
        timer.scheduleWithFixedDelay(()->Platform.runLater(()->{if(!busy && api!=null && store!=null && v.auto.isSelected() && System.currentTimeMillis()>=automaticRetryAfter)synchronize();}),60,60,TimeUnit.SECONDS);
    }
    private <T> void run(Callable<T> action,Consumer<T> success){
        if(busy)return;busy=true;controls();
        var task=new Task<T>(){@Override protected T call()throws Exception{return action.call();}};
        task.setOnSucceeded(e->{busy=false;controls();try{success.accept(task.getValue());}catch(Exception x){v.status.setText("Affichage impossible : "+x.getMessage());}});
        task.setOnFailed(e->{busy=false;
            if(task.getException() instanceof ApiClient.ApiException a && (a.status()==401 || a.status()==403)){if(api!=null)api.clear();api=null;v.session.setText("Session expirée ou accès refusé — coffre local ouvert ; reconnectez-vous après verrouillage.");}
            controls();Throwable x=task.getException();
            if(x instanceof java.io.IOException || (x instanceof ApiClient.ApiException a && (a.status()==429 || a.status()>=500))) {transientFailures=Math.min(transientFailures+1,6);automaticRetryAfter=System.currentTimeMillis()+Math.min(3600000L,60000L*(1L<<transientFailures));}
            v.status.setText(x instanceof IllegalArgumentException || x instanceof ApiClient.ApiException ? x.getMessage():"Opération interrompue. Les opérations locales sont conservées. Vérifiez réseau, serveur ou stockage.");
        });worker.submit(task);
    }
    private void controls(){
        boolean opened=store!=null;v.workspace.setDisable(busy||!opened);v.lock.setDisable(busy||!opened);
        for(var n:List.of(v.server,v.username,v.password,v.localPassword,v.login,v.offline))n.setDisable(busy||opened);
        v.refresh.setDisable(api==null);v.sync.setDisable(api==null);v.checkout.setDisable(api==null);v.reconcile.setDisable(api==null);
    }
    private String parish(){if(v.parish.getValue()==null)throw new IllegalArgumentException("Sélectionnez une paroisse.");return v.parish.getValue().id();}
    private record Opened(ApiClient api,LocalStore store,JsonNode session,JsonNode parishes){}
    private void unlock(boolean online){
        String username=v.username.getText().trim(),address=v.server.getText().trim(),password=v.password.getText();char[] local=v.localPassword.getText().toCharArray();
        if(username.isBlank() || local.length==0 || (online && password.isEmpty())){v.status.setText("Identifiant et mot de passe local requis ; mot de passe serveur requis en ligne.");return;}
        v.password.clear();v.localPassword.clear();
        run(()->{
            var client=new ApiClient(address);LocalStore opened=null;
            try {
                String normalized=client.baseUrl();JsonNode info;
                if(online){info=client.login(username,password);String owner=info.path("user").path("id").asText();if(owner.isBlank())throw new IllegalArgumentException("Session serveur sans identifiant utilisateur");
                    opened=LocalStore.unlock(file,normalized,username,local,owner);
                    JsonNode parishes=info.path("paroisses");
                    if(info.path("user").path("isGlobal").asBoolean())parishes=client.get("paroisses");
                    opened.put("session",info);opened.put("parishes",parishes);return new Opened(client,opened,info,parishes);
                }
                opened=LocalStore.unlock(file,normalized,username,local,null);info=opened.get("session");
                return new Opened(null,opened,info,opened.get("parishes"));
            }catch(Exception e){client.clear();if(opened!=null)opened.close();throw e;}finally{Arrays.fill(local,'\0');}
        },o->{api=o.api();store=o.store();session=o.session();loading=true;v.parish.getItems().clear();
            for(var p:o.parishes()){String id=p.path("id").asText(p.path("publicId").asText());if(!id.isBlank() && p.path("active").asBoolean(true))v.parish.getItems().add(new Choice(id,p.path("nom").asText(p.path("paroisseNom").asText()),p));}
            if(!v.parish.getItems().isEmpty())v.parish.getSelectionModel().selectFirst();loading=false;
            v.session.setText((api==null?"Hors ligne — ":"En ligne — ")+session.path("user").path("username").asText(username));controls();
            v.status.setText("Coffre déverrouillé. "+(api==null?"Les droits seront revalidés à la synchronisation.":"Actualisez le catalogue avant de créer une demande."));
            changeParish();});
    }
    private void lock(){
        var client=api;run(()->{try{if(client!=null)client.logout();}catch(Exception ignored){}finally{if(store!=null)store.close();}return true;},ignored->{store=null;api=null;session=null;catalog=null;editing=null;v.parish.getItems().clear();v.demands.getItems().clear();v.outbox.getItems().clear();v.type.getItems().clear();v.plan.getItems().clear();v.schedule.getItems().clear();v.payment.getItems().clear();reset();v.detail.setText("");v.queueDetail.setText("");v.session.setText("Coffre verrouillé");v.status.setText("Session locale effacée. Les données restent chiffrées sur disque.");controls();});
    }
    private record Snapshot(JsonNode catalogue,JsonNode demands,List<ObjectNode> entries){}
    private Snapshot snapshot(String parish)throws Exception{return new Snapshot(store.get("catalogue:"+parish),store.get("demands:"+parish),store.entries());}
    private void changeParish(){if(store==null || v.parish.getValue()==null)return;String parish=parish();reset();run(()->snapshot(parish),this::display);}
    private void display(Snapshot s){
        catalog=s.catalogue();loading=true;setChoices(v.type,catalog.path("types"),"libelle");setChoices(v.schedule,catalog.path("horaires"),"libelle");setChoices(v.payment,catalog.path("paiements"),"libelle");loading=false;fillPlans();
        v.demands.getItems().clear();for(var d:s.demands())v.demands.getItems().add(new Row(d,description(d)));
        v.outbox.getItems().clear();for(var e:s.entries())if(!"ARCHIVED".equals(e.path("state").asText())) {
            JsonNode p=e.path("payload");String associated=p.path("paroissePublicId").asText(e.path("parish").asText());
            if(associated.isBlank() || associated.equals(parish()))v.outbox.getItems().add(new Row(e,stateLabel(e.path("state").asText())+" — "+kindLabel(e.path("kind").asText())+" — "+e.path("result").path("codeSuivie").asText(e.path("codeSuivie").asText(p.path("intention").asText()))));
        }
    }
    private String stateLabel(String state){return switch(state){case "DRAFT"->"Brouillon";case "PENDING"->"En attente";case "SENT"->"Confirmé";case "REJECTED"->"À corriger";default->state;};}
    private String kindLabel(String kind){return switch(kind){case "CREATE_DEMANDE"->"Nouvelle demande";case "CASH_PAYMENT"->"Encaissement espèces";case "UPDATE_INTENTION"->"Correction d'intention";case "VALIDATE_DEMANDE"->"Décision";case "DELETE_DEMANDE"->"Suppression";default->kind;};}
    private void mutate(String kind){try{
        requirePermission(switch(kind){case "UPDATE_INTENTION"->"demand:edit";case "VALIDATE_DEMANDE"->"demand:validate";default->"demand:delete";});
        var d=selected(v.demands);String text=null,decision=null;
        if("UPDATE_INTENTION".equals(kind)){var dialog=new TextInputDialog(d.path("intention").asText());dialog.setTitle("Corriger l'intention");dialog.setHeaderText("Correction envoyée à la prochaine synchronisation");var value=dialog.showAndWait();if(value.isEmpty())return;text=value.get().trim();if(text.length()<10 || text.length()>500)throw new IllegalArgumentException("L'intention doit contenir 10 à 500 caractères.");}
        else if("VALIDATE_DEMANDE".equals(kind)){var dialog=new ChoiceDialog<>("Valider",List.of("Valider","Rejeter"));dialog.setTitle("Décision sur la demande");dialog.setHeaderText(d.path("codeSuivie").asText());var value=dialog.showAndWait();if(value.isEmpty())return;decision="Valider".equals(value.get())?"VALIDEE":"REJETEE";}
        if(!confirm("Mettre en attente cette opération sur "+d.path("codeSuivie").asText()+" ? Elle sera refusée si la demande a changé sur le serveur."))return;
        String intention=text,statut=decision,parish=parish();run(()->{store.queueMutation(kind,d,intention,statut);return snapshot(parish);},s->{display(s);v.tabs.getSelectionModel().select(2);v.status.setText("Opération mise en file. Le serveur vérifiera vos droits et la version de la demande.");});
    }catch(Exception e){v.status.setText(e.getMessage());}}
    private String description(JsonNode d){return d.path("codeSuivie").asText()+" — "+d.path("montant").asText()+" XOF — "+d.path("statutPaiement").asText()+" — "+d.path("statutValidation").asText()+" — "+d.path("intention").asText();}
    private void setChoices(ComboBox<Choice> box,JsonNode values,String label){box.getItems().clear();for(var value:values)if(value.path("isActive").asBoolean(true) && !value.path("statusDel").asBoolean()){String text=value.path(label).asText(value.path("publicId").asText());if(value.has("heureCelebration"))text+=" — "+value.path("jourSemaine").asText()+" "+value.path("heureCelebration").asText();box.getItems().add(new Choice(value.path("publicId").asText(),text,value));}if(!box.getItems().isEmpty())box.getSelectionModel().selectFirst();}
    private void fillPlans(){if(catalog==null || v.type.getValue()==null){v.plan.getItems().clear();return;}setChoices(v.plan,catalog.path("forfaits").path(v.type.getValue().id()),"nomForfait");price();}
    private void requirePermission(String permission){
        if(session!=null)for(var p:session.path("user").path("permissions"))if(permission.equals(p.asText()))return;
        throw new IllegalArgumentException("Votre rôle ne permet pas cette opération : "+permission);
    }
    private void price(){v.price.setText(v.plan.getValue()==null?"Aucun forfait disponible":"Tarif indicatif : "+v.plan.getValue().data().path("montantForfait").asText()+" XOF — le serveur revalide le tarif.");}
    private void reset(){editing=null;configuredSlots=null;v.slotsSummary.setText("Horaire commun par défaut");v.surname.clear();v.firstname.clear();v.phone.clear();v.email.clear();v.courier.clear();v.intention.clear();v.extraDates.clear();v.customTime.clear();v.date.setValue(LocalDate.now().plusDays(2));}
    private String required(ComboBox<Choice> box,String label){if(box.getValue()==null)throw new IllegalArgumentException(label+" obligatoire.");return box.getValue().id();}
    private ObjectNode payload(){
        if(v.intention.getText().trim().length()<10 || v.intention.getText().trim().length()>500)throw new IllegalArgumentException("L'intention doit contenir 10 à 500 caractères.");
        if(!v.phone.getText().trim().matches("\\+[1-9]\\d{6,14}"))throw new IllegalArgumentException("Téléphone au format international requis, par exemple +22890123456.");
        if(v.date.getValue()==null)throw new IllegalArgumentException("Date obligatoire.");
        var p=json.createObjectNode().put("paroissePublicId",parish()).put("typeDemandePublicId",required(v.type,"Type de demande")).put("forfaitTarifPublicId",required(v.plan,"Forfait")).put("typePaiementPublicId",required(v.payment,"Mode de paiement"));
        p.put("nomFidele",v.surname.getText().trim()).put("prenomFidele",v.firstname.getText().trim()).put("telFidele",v.phone.getText().trim()).put("emailFidele",v.email.getText().trim()).put("nomCoursier",v.courier.getText().trim()).put("intention",v.intention.getText().trim()).put("dateDebut",v.date.getValue().toString());
        var dates=new TreeSet<LocalDate>();dates.add(v.date.getValue());if(!v.extraDates.getText().isBlank())for(String raw:v.extraDates.getText().split(","))dates.add(LocalDate.parse(raw.trim()));
        int required=v.plan.getValue().data().path("nombreCelebration").asInt(1);if(dates.size()!=required)throw new IllegalArgumentException("Le forfait exige "+required+" date(s) distincte(s).");
        ArrayNode list=p.putArray("datesCelebration"),slots=p.putArray("celebrationSlots");
        if(!v.customTime.getText().isBlank())p.put("heurePersonnalisee",LocalTime.parse(v.customTime.getText().trim()).toString());else if(configuredSlots==null)p.put("horairePublicId",required(v.schedule,"Horaire"));
        for(LocalDate d:dates){list.add(d.toString());ObjectNode slot=slots.addObject().put("date",d.toString());if(p.has("heurePersonnalisee"))slot.set("heurePersonnalisee",p.get("heurePersonnalisee"));else slot.set("horairePublicId",p.get("horairePublicId"));}
        if(configuredSlots!=null){
            var configuredDates=new TreeSet<LocalDate>();for(var slot:configuredSlots)configuredDates.add(LocalDate.parse(slot.path("date").asText()));
            if(!configuredDates.equals(dates))throw new IllegalArgumentException("Les dates ont changé : reconfigurez les créneaux par date.");
            p.set("celebrationSlots",configuredSlots.deepCopy());var first=configuredSlots.get(0);p.remove("heurePersonnalisee");p.remove("horairePublicId");
            if(first.has("heurePersonnalisee"))p.set("heurePersonnalisee",first.get("heurePersonnalisee"));else p.set("horairePublicId",first.get("horairePublicId"));
        }
        return p;
    }
    private void configureSlots(){try{
        if(v.date.getValue()==null)throw new IllegalArgumentException("Date obligatoire");
        var dates=new TreeSet<LocalDate>();dates.add(v.date.getValue());if(!v.extraDates.getText().isBlank())for(String value:v.extraDates.getText().split(","))dates.add(LocalDate.parse(value.trim()));
        if(dates.size()>30)throw new IllegalArgumentException("30 dates au maximum.");
        var dialog=new Dialog<ArrayNode>();dialog.setTitle("Créneaux de célébration");dialog.setHeaderText("Choisir l'horaire ou l'heure personnalisée pour chaque date. Le serveur vérifiera leur disponibilité.");dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL,ButtonType.OK);
        var grid=new javafx.scene.layout.GridPane();grid.setHgap(8);grid.setVgap(8);var choices=new ArrayList<ComboBox<Choice>>();var hours=new ArrayList<TextField>();int row=0;
        for(var date:dates){var box=new ComboBox<Choice>();box.getItems().setAll(v.schedule.getItems());box.setValue(v.schedule.getValue());var time=new TextField(v.customTime.getText());time.setPromptText("Heure personnalisée HH:mm");grid.addRow(row++,new Label(date.toString()),box,time);choices.add(box);hours.add(time);}
        dialog.getDialogPane().setContent(new ScrollPane(grid));dialog.setResultConverter(button->{if(button!=ButtonType.OK)return null;try{var slots=json.createArrayNode();int i=0;for(var date:dates){var slot=slots.addObject().put("date",date.toString());String time=hours.get(i).getText().trim();if(!time.isBlank())slot.put("heurePersonnalisee",LocalTime.parse(time).toString());else slot.put("horairePublicId",required(choices.get(i),"Horaire du "+date));i++;}return slots;}catch(Exception invalid){v.status.setText("Créneau invalide : "+invalid.getMessage());return null;}});
        var result=dialog.showAndWait();if(result.isPresent()){configuredSlots=result.get();v.slotsSummary.setText(configuredSlots.size()+" créneau(x) configuré(s)");}
    }catch(Exception e){v.status.setText(e.getMessage());}}
    private void save(){try{requirePermission("demand:edit");var p=payload();String id=editing,parish=parish();run(()->{store.draft(id,p);return snapshot(parish);},s->{display(s);reset();v.tabs.getSelectionModel().select(2);v.status.setText("Brouillon sauvegardé et chiffré. Mise en file d'envoi séparée.");});}catch(Exception e){v.status.setText(e.getMessage());}}
    private JsonNode selected(ListView<Row> list){if(list.getSelectionModel().getSelectedItem()==null)throw new IllegalArgumentException("Sélectionnez une ligne.");return list.getSelectionModel().getSelectedItem().data();}
    private boolean confirm(String text){return new Alert(Alert.AlertType.CONFIRMATION,text,ButtonType.CANCEL,ButtonType.OK).showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK;}
    private void queue(){try{requirePermission("demand:edit");String id=selected(v.outbox).path("id").asText(),parish=parish();if(!confirm("Envoyer cette demande au serveur lors de la prochaine synchronisation ? Le serveur revalidera les dates et le tarif."))return;run(()->{store.queue(id);return snapshot(parish);},s->{display(s);v.status.setText("Demande en attente de synchronisation.");});}catch(Exception e){v.status.setText(e.getMessage());}}
    private void archive(){try{String id=selected(v.outbox).path("id").asText(),parish=parish();if(!confirm("Archiver ce brouillon ou rejet ?"))return;run(()->{store.archive(id);return snapshot(parish);},this::display);}catch(Exception e){v.status.setText(e.getMessage());}}
    private void edit(){try{var e=selected(v.outbox);String state=e.path("state").asText();if(!"CREATE_DEMANDE".equals(e.path("kind").asText()) || !("DRAFT".equals(state)||"REJECTED".equals(state)))throw new IllegalArgumentException("Seuls les brouillons et demandes rejetées sont modifiables.");var p=e.path("payload");if(!parish().equals(p.path("paroissePublicId").asText()))throw new IllegalArgumentException("Sélectionnez d'abord la paroisse du brouillon.");editing="DRAFT".equals(state)?e.path("id").asText():null;choose(v.type,p.path("typeDemandePublicId").asText());fillPlans();choose(v.plan,p.path("forfaitTarifPublicId").asText());choose(v.schedule,p.path("horairePublicId").asText());choose(v.payment,p.path("typePaiementPublicId").asText());v.surname.setText(p.path("nomFidele").asText());v.firstname.setText(p.path("prenomFidele").asText());v.phone.setText(p.path("telFidele").asText());v.email.setText(p.path("emailFidele").asText());v.courier.setText(p.path("nomCoursier").asText());v.intention.setText(p.path("intention").asText());v.date.setValue(LocalDate.parse(p.path("dateDebut").asText()));v.customTime.setText(p.path("heurePersonnalisee").asText(""));var extra=new ArrayList<String>();for(var d:p.path("datesCelebration"))if(!d.asText().equals(p.path("dateDebut").asText()))extra.add(d.asText());v.extraDates.setText(String.join(", ",extra));configuredSlots=p.path("celebrationSlots").isArray()?(ArrayNode)p.path("celebrationSlots").deepCopy():null;v.slotsSummary.setText(configuredSlots==null?"Horaire commun par défaut":configuredSlots.size()+" créneau(x) configuré(s)");v.tabs.getSelectionModel().select(0);}catch(Exception e){v.status.setText(e.getMessage());}}
    private void choose(ComboBox<Choice> box,String id){box.setValue(null);for(var c:box.getItems())if(c.id().equals(id)){box.setValue(c);break;}}
    private void refresh(){if(api==null)return;try{String parish=parish();run(()->{var sync=new SyncService(store,api);sync.catalogue(parish);sync.refresh(parish);return snapshot(parish);},s->{display(s);v.status.setText("Catalogue et copie serveur actualisés.");});}catch(Exception e){v.status.setText(e.getMessage());}}
    private void synchronize(){if(api==null)return;String parish=v.parish.getValue()==null?null:parish();run(()->{var sync=new SyncService(store,api);String result=sync.synchronize();transientFailures=0;automaticRetryAfter=0;if(parish!=null)sync.refresh(parish);return result;},result->{v.status.setText(result);if(parish!=null)run(()->snapshot(parish),this::display);});}
    private void cash(){try{requirePermission("payment:manage");var demand=selected(v.demands);if("PAYE".equals(demand.path("statutPaiement").asText()))throw new IllegalArgumentException("Cette demande est déjà payée.");if(!confirm("Confirmez-vous avoir reçu "+demand.path("montant").asText()+" XOF en espèces pour "+demand.path("codeSuivie").asText()+" ? L'enregistrement reste provisoire jusqu'à validation serveur."))return;String parish=parish();run(()->{store.queueCash(demand);return snapshot(parish);},s->{display(s);v.tabs.getSelectionModel().select(2);v.status.setText("Encaissement provisoire mis en file. Aucun paiement distant n'est encore confirmé.");});}catch(Exception e){v.status.setText(e.getMessage());}}
    private void receipt(){try{var demand=selected(v.demands);String code=demand.path("codeSuivie").asText();if(code.isBlank())throw new IllegalArgumentException("Code de suivi absent");var chooser=new FileChooser();chooser.setTitle("Exporter le reçu (fichier non chiffré)");chooser.setInitialFileName("recu-"+code.replaceAll("[^A-Za-z0-9_-]","_")+".pdf");chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF","*.pdf"));var target=chooser.showSaveDialog(v.root.getScene().getWindow());if(target==null)return;boolean online=api!=null;run(()->{byte[] pdf=new SyncService(store,api).receipt(code,online);Files.write(target.toPath(),pdf);return target.toURI().toString();},uri->{if(host!=null)host.showDocument(uri);v.status.setText("PDF exporté. Impression depuis votre lecteur PDF. Ce fichier exporté n'est pas chiffré.");});}catch(Exception e){v.status.setText(e.getMessage());}}
    private void checkout(){try{var d=selected(v.demands);if(api==null)throw new IllegalArgumentException("Connexion Internet requise.");String code=d.path("codeSuivie").asText();if(!confirm("Préparer le paiement en ligne de "+code+" ? Le prestataire affichera le montant avec ses frais."))return;run(()->api.post("paiements/checkout/"+java.net.URLEncoder.encode(code,java.nio.charset.StandardCharsets.UTF_8),json.createObjectNode()),result->{String url=result.path("paymentUrl").asText();if(!url.isBlank()){var uri=java.net.URI.create(url);if(!"https".equals(uri.getScheme()) || uri.getHost()==null || uri.getUserInfo()!=null){v.status.setText("URL de paiement refusée.");return;}if(host!=null)host.showDocument(url);v.status.setText("Paiement ouvert dans le navigateur. Cliquez ensuite sur Vérifier le paiement.");}else v.status.setText(result.path("message").asText("Pas d'URL de paiement disponible."));});}catch(Exception e){v.status.setText(e.getMessage());}}
    private void reconcile(){try{String code=selected(v.demands).path("codeSuivie").asText();run(()->api.post("paiements/reconcile/"+java.net.URLEncoder.encode(code,java.nio.charset.StandardCharsets.UTF_8),json.createObjectNode()),r->{v.status.setText("État vérifié : "+r.path("statutPaiement").asText());refresh();});}catch(Exception e){v.status.setText(e.getMessage());}}
    @Override public void close(){timer.shutdownNow();worker.shutdownNow();if(api!=null)api.clear();if(store!=null)store.close();}
}
