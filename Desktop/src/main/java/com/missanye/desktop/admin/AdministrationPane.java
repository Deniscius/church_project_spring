package com.missanye.desktop.admin;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.missanye.desktop.admin.ParishModules.Module;
import com.missanye.desktop.model.ScreenFormats;
import com.missanye.desktop.service.OperationErrors;
import com.missanye.desktop.view.WorkbenchView;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import static com.missanye.desktop.admin.ParishModules.*;

/** Native parish administration: named controls, permission-aware actions and server-confirmed writes. */
public final class AdministrationPane extends BorderPane implements AutoCloseable {
    private static final ObjectMapper JSON=new ObjectMapper();
    private final ParishAdministration service;
    private final WorkbenchView view;
    private final Runnable changed,passwordChanged;
    private final BooleanSupplier active;
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"parish-administration");t.setDaemon(true);return t;});
    private final ComboBox<Module> modules=new ComboBox<>();
    private final TableView<JsonNode> table=new TableView<>();
    private final TextField search=new TextField();
    private final ObservableList<JsonNode> rows=FXCollections.observableArrayList();
    private final GridPane form=new GridPane();
    private final VBox summary=new VBox(7),editor=new VBox(12);
    private final Label message=new Label();
    private final Button save=new Button("Enregistrer"),fresh=new Button("Nouveau"),remove=new Button("Supprimer"),refresh=new Button("Actualiser");
    private final FlowPane extras=new FlowPane(8,8);
    private final DatePicker date=new DatePicker(LocalDate.now(ZoneId.of("Africa/Lome")));
    private final Map<String,Node> controls=new LinkedHashMap<>();
    private final Map<String,CheckBox> dayBoxes=new LinkedHashMap<>();
    private final Map<String,Supplier<JsonNode>> values=new LinkedHashMap<>();
    private JsonNode selected,data=JSON.nullNode(),types=JSON.createArrayNode(),daySlots=JSON.createArrayNode();
    private boolean filling,internal,dirty,busy,closed,changingDate;
    private LocalDate loadedDate=date.getValue();
    private long generation;

    public AdministrationPane(ParishAdministration service,WorkbenchView view,BooleanSupplier active,Runnable changed,Runnable passwordChanged){this(service,view,active,changed,passwordChanged,null);}
    public AdministrationPane(ParishAdministration service,WorkbenchView view,BooleanSupplier active,Runnable changed,Runnable passwordChanged,String initialModule){
        this.service=service;this.view=view;this.active=active;this.changed=changed;this.passwordChanged=passwordChanged;
        setId("parishAdministration");setPrefSize(940,650);save.getStyleClass().add("primary-button");remove.getStyleClass().add("danger-button");table.getStyleClass().add("data-table");
        modules.getItems().setAll(service.modules());modules.setMaxWidth(Double.MAX_VALUE);modules.setId("administrationModule");
        search.setPromptText("Rechercher dans cette rubrique");search.setId("administrationSearch");
        var top=new VBox(10,new Label("Parcours et paramètres de la paroisse"),new HBox(12,modules,refresh),search);setTop(top);BorderPane.setMargin(top,new javafx.geometry.Insets(0,0,12,0));
        var filtered=new FilteredList<>(rows);var sorted=new SortedList<>(filtered);sorted.comparatorProperty().bind(table.comparatorProperty());table.setItems(sorted);table.setPrefHeight(220);table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);table.setPlaceholder(new Label("Aucun élément dans cette rubrique."));
        search.textProperty().addListener((o,a,b)->filtered.setPredicate(row->searchable(row).contains(b.strip().toLowerCase(Locale.FRANCE))));
        form.setHgap(12);form.setVgap(12);form.getColumnConstraints().addAll(new ColumnConstraints(220),new ColumnConstraints());form.getColumnConstraints().get(1).setHgrow(Priority.ALWAYS);
        message.setWrapText(true);message.setId("administrationMessage");save.setId("administrationSave");fresh.setId("administrationNew");remove.setId("administrationDelete");
        editor.getChildren().addAll(summary,form,extras,new FlowPane(8,8,fresh,save,remove),message);
        var content=new VBox(12,table,editor);var scroll=new ScrollPane(content);scroll.setFitToWidth(true);scroll.setPrefViewportHeight(550);setCenter(scroll);
        modules.valueProperty().addListener((o,old,value)->{if(internal)return;if(dirty&&!discard()){internal=true;modules.setValue(old);internal=false;return;}dirty=false;load();});
        table.getSelectionModel().selectedItemProperty().addListener((o,old,value)->{if(internal||value==null)return;if(dirty&&!discard()){internal=true;table.getSelectionModel().select(old);internal=false;return;}selected=value;showForm();});
        refresh.setOnAction(e->{if(!dirty||discard()){dirty=false;load();}});fresh.setOnAction(e->{if(!dirty||discard()){selected=null;internal=true;table.getSelectionModel().clearSelection();internal=false;showForm();}});
        save.setOnAction(e->save());remove.setOnAction(e->{if(selected!=null&&confirm("Supprimer cet élément ? Les règles métier du serveur restent applicables."))execute(()->{service.remove(module(),selected);return JSON.nullNode();},r->{changed.run();dirty=false;load();});});
        if(!modules.getItems().isEmpty())modules.setValue(modules.getItems().stream().filter(m->m.id().equals(initialModule)).findFirst().orElse(modules.getItems().get(0)));else message.setText("Votre compte ne dispose d’aucune rubrique de configuration.");buttons();
    }
    private Module module(){return modules.getValue();}
    private String searchable(JsonNode row){var text=new StringBuilder();row.fields().forEachRemaining(e->{if(e.getValue().isValueNode()&&!e.getKey().toLowerCase().contains("password"))text.append(e.getValue().asText()).append(' ');});return text.toString().toLowerCase(Locale.FRANCE);}
    private boolean confirm(String text){return view.decorate(new Alert(Alert.AlertType.CONFIRMATION,text,ButtonType.OK,ButtonType.CANCEL)).showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK;}
    private boolean discard(){return confirm("Abandonner les modifications non enregistrées de ce formulaire ?");}
    public boolean mayClose(){if(busy){message.setText("Attendez la confirmation de l’opération avant de fermer.");return false;}if(dirty&&!discard())return false;dirty=false;return true;}
    private <T> void execute(Callable<T> action,Consumer<T> success){
        if(closed||busy||!active.getAsBoolean())return;busy=true;buttons();message.setText("Chargement en cours…");long token=++generation;
        var task=new Task<T>(){protected T call()throws Exception{return action.call();}};
        task.setOnSucceeded(e->{if(closed||token!=generation||!active.getAsBoolean())return;busy=false;message.setText(service.online()?"Données confirmées par le serveur.":"Dernières données conservées sur ce poste. Reconnectez-vous pour les modifier.");success.accept(task.getValue());buttons();});
        task.setOnFailed(e->{if(closed||token!=generation||!active.getAsBoolean())return;busy=false;message.setText(OperationErrors.message(task.getException()));buttons();});worker.submit(task);
    }
    private void load(){
        if(module()==null||busy)return;var chosen=module();LocalDate selectedDate=date.getValue();if(selectedDate==null){message.setText("Choisissez une date valide.");return;}record Loaded(JsonNode data,JsonNode types,JsonNode slots){}
        execute(()->new Loaded(service.load(chosen,selectedDate),chosen.id().equals("prices")?service.types():JSON.createArrayNode(),chosen.id().equals("celebrations")?service.daySlots(selectedDate):JSON.createArrayNode()),loaded->{loadedDate=selectedDate;data=loaded.data();types=loaded.types();daySlots=loaded.slots();selected=null;rows.clear();
            if(data.isArray())data.forEach(rows::add);else if(chosen.id().equals("cash")&&data.path("encaissements").isArray())data.path("encaissements").forEach(rows::add);
            else if(data.path("content").isArray())data.path("content").forEach(rows::add);else if(!chosen.singleton()&&data.isObject())rows.add(data);
            columns(chosen);if(chosen.singleton())selected=data;showForm();
        });
    }
    private void columns(Module chosen){
        table.getColumns().clear();List<String> keys=switch(chosen.id()){
            case "schedules"->List.of("libelle","jourSemaine","dateSpecifique","heureCelebration","natureHonoraire","isActive");
            case "types"->List.of("libelle","typeDemandeEnum","delaiMinimumHeures","isActive");
            case "prices"->List.of("nomForfait","natureForfait","montantForfait","nombreCelebration","isActive");
            case "team"->List.of("nom","prenom","username","role","isActive");
            case "choirs"->List.of("nom","description");
            case "transfers"->List.of("createdAt","montant","statut","motif","referenceVirement");
            case "cash"->List.of("dateEncaissement","encaisseurNom","codeSuivie","fidele","montant");
            case "invoices"->List.of("refFacture","codeSuivieDemande","montant","statutPaiement","dateEmission");
            case "archives"->List.of("codeSuivie","nomFidele","statutDemande","deletedAt","deletedByNom");
            case "payments"->List.of("codeSuivie","nomFidele","montant","statutPaiement","modePaiement");
            case "celebrations"->List.of("heureCelebration","codeSuivie","fidele","intention","statutPaiement","celebre");
            case "programme"->List.of("date","jourLibelle","modeProgramme");default->List.of();};
        for(String key:keys){var column=new TableColumn<JsonNode,String>(fieldLabel(key));column.setCellValueFactory(c->new ReadOnlyStringWrapper(format(key,c.getValue().path(key))));table.getColumns().add(column);}
        table.setManaged(!chosen.singleton());table.setVisible(!chosen.singleton());search.setManaged(!chosen.singleton());search.setVisible(!chosen.singleton());
    }
    private String fieldLabel(String key){var current=module();if(current!=null)for(var field:current.fields())if(field.key().equals(key))return field.label();return switch(key){case "createdAt","dateEmission","dateEncaissement"->"Date";case "montant"->"Montant (XOF)";case "statut","statutPaiement"->"Statut";case "codeSuivie","codeSuivieDemande"->"Code de suivi";case "encaisseurNom"->"Encaisseur";case "fidele","nomFidele"->"Fidèle";case "referenceVirement"->"Référence de virement";case "refFacture"->"Référence facture";case "modePaiement"->"Mode de paiement";case "date"->"Date";case "jourLibelle"->"Jour";case "modeProgramme"->"Programme";case "celebre"->"Célébrée";case "heureCelebration"->"Heure de célébration";case "soldeDisponible"->"Solde disponible";case "soldeEnAttente"->"Solde en attente";case "total"->"Total encaissé";case "totalDuJour"->"Caisse du jour";case "totalDuMois"->"Caisse du mois";case "nombreEncaissements"->"Encaissements";case "prenomFidele"->"Prénoms du fidèle";case "telFidele"->"Téléphone du fidèle";case "emailFidele"->"E-mail du fidèle";case "intention"->"Intention";case "typeDemandeLibelle"->"Type de demande";case "forfaitNom"->"Forfait";case "statutDemande"->"État de la demande";case "statutValidation"->"Validation";case "datePaiement"->"Date de paiement";case "typePaiementLibelle"->"Type de paiement";case "nom"->"Paroisse";case "adresse"->"Adresse";case "deletedAt"->"Date d’archivage";case "deletedByNom"->"Archivé par";case "montantCharge"->"Montant débité";case "montantFrais"->"Total des frais";case "montantFraisAgregateur"->"Frais du prestataire";case "montantFraisPlateforme"->"Frais de la plateforme";case "montantNet"->"Montant net paroisse";case "provider"->"Prestataire";case "idTransaction"->"Transaction";case "numeroPayeur"->"Numéro du payeur";default->"Information";};}
    private String format(String key,JsonNode value){if(value.isNull()||value.isMissingNode())return "—";if(value.isBoolean())return value.asBoolean()?"Oui":"Non";if(value.isArray()){var text=new ArrayList<String>();value.forEach(v->text.add(label(v.asText())));return String.join(", ",text);}if(key.toLowerCase().contains("montant")||key.startsWith("solde")||key.startsWith("total"))return ScreenFormats.money(value);String text=value.asText();if(key.contains("At")||key.startsWith("date"))return ScreenFormats.date(text);return text.matches("[A-Z_]+")?label(text):text;}
    private void showForm(){
        filling=true;dirty=false;values.clear();controls.clear();dayBoxes.clear();form.getChildren().clear();summary.getChildren().clear();extras.getChildren().clear();var chosen=module();JsonNode initial=selected==null?JSON.createObjectNode():selected;
        if(chosen.id().equals("profile"))summary.getChildren().add(new Label("Identifiant : "+data.path("username").asText("")+" · "+label(data.path("role").asText(""))));
        if(chosen.id().equals("parish"))summary.getChildren().addAll(new Label("Paroisse : "+data.path("nom").asText("")),new Label("Adresse : "+data.path("adresse").asText("")));
        if(chosen.id().equals("account")||chosen.id().equals("cash"))for(String key:List.of("soldeDisponible","soldeEnAttente","total","totalDuJour","totalDuMois","nombreEncaissements"))if(data.has(key))summary.getChildren().add(new Label(fieldLabel(key)+" : "+format(key,data.get(key))));
        int row=0;for(var field:chosen.fields()){
            JsonNode value=initial.path(field.key());var node=control(field,value);controls.put(field.key(),node);node.setId("admin-"+field.key());
            if(List.of("profile","parish").contains(chosen.id())&&field.key().equals("email"))node.setDisable(true);
            if(chosen.id().equals("team")&&selected!=null&&List.of("nom","prenom","username","email","telephone","password","roleParoisse").contains(field.key()))node.setDisable(true);
            var fieldName=new Label(field.label()+(field.required()?" *":""));fieldName.setWrapText(true);fieldName.setMaxWidth(215);fieldName.setMinHeight(Region.USE_PREF_SIZE);form.add(fieldName,0,row);form.add(node,1,row++);GridPane.setHgrow(node,Priority.ALWAYS);
        }
        if(chosen.id().equals("prices")){
            var typeChoice=(ComboBox<?>)controls.get("typeDemandePublicId");Runnable limitDays=()->{String typeId=values.get("typeDemandePublicId").get().asText("");var allowed=new LinkedHashSet<String>();for(var type:types)if(typeId.equals(type.path("publicId").asText()))type.path("joursCelebrationAutorises").forEach(day->allowed.add(day.asText()));dayBoxes.forEach((day,box)->{box.setDisable(!allowed.contains(day));if(!allowed.contains(day))box.setSelected(false);});if(dayBoxes.values().stream().noneMatch(CheckBox::isSelected))dayBoxes.forEach((day,box)->box.setSelected(allowed.contains(day)));};typeChoice.valueProperty().addListener((o,a,b)->limitDays.run());limitDays.run();
        }
        if(chosen.fields().isEmpty()&&selected!=null&&!chosen.id().equals("programme")){
            for(String key:List.of("refFacture","codeSuivie","codeSuivieDemande","nomFidele","prenomFidele","telFidele","emailFidele","intention","typeDemandeLibelle","forfaitNom","montant","statutDemande","statutPaiement","statutValidation","typePaiementLibelle","modePaiement","dateEmission","datePaiement","dateEncaissement","encaisseurNom","referenceVirement","motif","createdAt"))if(selected.hasNonNull(key)){
                var label=new Label(fieldLabel(key)+" : "+format(key,selected.get(key)));label.setWrapText(true);summary.getChildren().add(label);
            }
        }
        if(selected!=null&&selected.path("reglement").isObject()){
            summary.getChildren().add(new Label("Règlement effectif de la facture"));for(String key:List.of("datePaiement","statut","montantCharge","montantFrais","montantFraisAgregateur","montantFraisPlateforme","montantNet","provider","idTransaction","numeroPayeur"))if(selected.path("reglement").hasNonNull(key)){var label=new Label(fieldLabel(key)+" : "+format(key,selected.path("reglement").get(key)));label.setWrapText(true);summary.getChildren().add(label);}
        }
        if(chosen.id().equals("programme"))programme();
        if(chosen.id().equals("celebrations")){
            date.setOnAction(e->changeDate());summary.getChildren().add(new HBox(8,new Label("Date des célébrations"),date));
            if(selected!=null){
                record Slot(String id,String label){@Override public String toString(){return label;}}
                var choices=new ComboBox<Slot>();for(var slot:daySlots)choices.getItems().add(new Slot(slot.path("horairePublicId").asText(),slot.path("heureCelebration").asText()+" · "+slot.path("libelle").asText("")));
                if(!choices.getItems().isEmpty())choices.getSelectionModel().selectFirst();summary.getChildren().add(choices);
                extra("Déplacer cette célébration",selected.path("modifiable").asBoolean()&&service.allowed("celebration-schedule:manage"),()->{if(choices.getValue()!=null&&confirm("Confirmer le nouvel horaire de cette célébration ?"))execute(()->service.moveCelebration(selected,choices.getValue().id()),r->{changed.run();load();});});
                extra("Confirmer la célébration",!selected.path("celebre").asBoolean()&&service.allowed("celebration:manage"),()->{if(confirm("Confirmez-vous que cette intention a été célébrée ?"))execute(()->service.markCelebrated(selected),r->{changed.run();load();});});
            }
        }
        if(chosen.id().equals("parish")){
            extra("Choisir le logo",service.allowed("parish-settings:manage"),()->{var chooser=new FileChooser();chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images PNG / JPEG","*.png","*.jpg","*.jpeg"));var image=chooser.showOpenDialog(getScene().getWindow());if(image!=null)execute(()->service.logo(image.toPath()),r->{changed.run();load();});});
            extra("Retirer le logo",service.allowed("parish-settings:manage"),()->{if(confirm("Retirer le logo du reçu ?"))execute(()->service.logo(null),r->{changed.run();load();});});
            extra("Exporter le reçu modèle",service.allowed("parish:read"),()->{var chooser=new FileChooser();chooser.setInitialFileName("recu-modele.pdf");var file=chooser.showSaveDialog(getScene().getWindow());if(file!=null)execute(()->{com.missanye.desktop.service.ReceiptExporter.write(file.toPath(),service.sampleReceipt());return true;},r->message.setText("Reçu modèle enregistré."));});
        }
        if(chosen.id().equals("choirs"))extra("Inviter dans Chorale Management",selected!=null&&service.allowed("choir:invite"),()->execute(()->service.invite(selected),r->{var token=r.path("token").asText("");var dialog=view.decorate(new Dialog<Void>());dialog.setTitle("Invitation de la chorale");dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);var text=new TextField(token);text.setEditable(false);var copy=new Button("Copier l’invitation");copy.setOnAction(e->{var content=new javafx.scene.input.ClipboardContent();content.putString(token);javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);});dialog.getDialogPane().setContent(new VBox(10,new Label("Invitation à transmettre au responsable de la chorale"),text,new Label("Expire le : "+r.path("expiresAt").asText("")),copy));dialog.showAndWait();}));
        if(chosen.id().equals("team")&&selected!=null)summary.getChildren().add(new Label("Le titulaire gère son identité et son mot de passe dans Mon profil. Ici : rôle et activation."));
        filling=false;buttons();
    }
    private void extra(String title,boolean allowed,Runnable action){if(!allowed)return;var button=new Button(title);button.setDisable(!service.online());button.setOnAction(e->action.run());extras.getChildren().add(button);}
    private Node control(Field field,JsonNode initial){
        switch(field.kind()){
            case BANK->{var bank=new ComboBox<String>();bank.setEditable(true);bank.setMaxWidth(Double.MAX_VALUE);bank.getItems().addAll(field.options());bank.setValue(initial.asText(""));bank.getEditor().textProperty().addListener((o,a,b)->changedField());values.put(field.key(),()->JSON.getNodeFactory().textNode(bank.getEditor().getText().strip()));return bank;}
            case BOOLEAN->{var box=new CheckBox();box.setSelected(initial.isMissingNode()?field.key().equals("isActive"):initial.asBoolean());box.selectedProperty().addListener((o,a,b)->changedField());values.put(field.key(),()->JSON.getNodeFactory().booleanNode(box.isSelected()));return box;}
            case DATE->{var picker=new DatePicker();if(!initial.asText("").isBlank())try{picker.setValue(LocalDate.parse(initial.asText()));}catch(Exception ignored){}picker.valueProperty().addListener((o,a,b)->changedField());picker.getEditor().textProperty().addListener((o,a,b)->changedField());values.put(field.key(),()->{String text=picker.getEditor().getText();if(text==null||text.isBlank())return JSON.nullNode();try{return JSON.getNodeFactory().textNode(picker.getConverter().fromString(text).toString());}catch(Exception invalid){throw new IllegalArgumentException("Vérifiez la date : "+field.label());}});return picker;}
            case DAYS->{var pane=new FlowPane(8,8);var boxes=new LinkedHashMap<String,CheckBox>();for(String day:DAYS){var box=new CheckBox(label(day));for(var d:initial)if(day.equals(d.asText()))box.setSelected(true);box.selectedProperty().addListener((o,a,b)->changedField());boxes.put(day,box);dayBoxes.put(day,box);pane.getChildren().add(box);}values.put(field.key(),()->{var result=JSON.createArrayNode();boxes.forEach((day,box)->{if(box.isSelected())result.add(day);});return result;});return pane;}
            case CHOICE,TYPE->{record Choice(String id,String label){@Override public String toString(){return label;}}var combo=new ComboBox<Choice>();combo.setMaxWidth(Double.MAX_VALUE);if(!field.required())combo.getItems().add(new Choice("","—"));if(field.kind()==Kind.TYPE){for(var type:types)combo.getItems().add(new Choice(type.path("publicId").asText(),type.path("libelle").asText()));}else for(String option:field.options())combo.getItems().add(new Choice(option,label(option)));for(var choice:combo.getItems())if(choice.id().equals(initial.asText("")))combo.setValue(choice);if(combo.getValue()==null&&!combo.getItems().isEmpty())combo.getSelectionModel().selectFirst();combo.valueProperty().addListener((o,a,b)->changedField());values.put(field.key(),()->combo.getValue()==null||combo.getValue().id().isBlank()?JSON.nullNode():JSON.getNodeFactory().textNode(combo.getValue().id()));return combo;}
            default->{TextInputControl input=field.kind()==Kind.MULTILINE?new TextArea():field.kind()==Kind.PASSWORD?new PasswordField():new TextField();if(input instanceof TextArea area){area.setPrefRowCount(3);area.setWrapText(true);}input.setText(field.kind()==Kind.PASSWORD?"":initial.asText(""));if(field.kind()==Kind.TIME)input.setPromptText("06:30");if(initial.isMissingNode()&&field.key().equals("delaiMinimumHeures"))input.setText("24");if(initial.isMissingNode()&&field.key().equals("nombreCelebration"))input.setText("1");input.textProperty().addListener((o,a,b)->changedField());values.put(field.key(),()->parse(field,input.getText()));return input;}
        }
    }
    private JsonNode parse(Field field,String raw){String text=raw.strip();if(text.isBlank()){if(field.required())throw new IllegalArgumentException("Renseignez : "+field.label());return JSON.nullNode();}try{return switch(field.kind()){case INTEGER->JSON.getNodeFactory().numberNode(Integer.parseInt(text));case MONEY->JSON.getNodeFactory().numberNode(new java.math.BigDecimal(text.replace(',','.')));case TIME->JSON.getNodeFactory().textNode(LocalTime.parse(text).toString());default->JSON.getNodeFactory().textNode(text);};}catch(Exception invalid){throw new IllegalArgumentException("Valeur invalide : "+field.label());}}
    private void changedField(){if(!filling)dirty=true;}
    private void save(){
        try{var body=JSON.createObjectNode();values.forEach((key,get)->body.set(key,get.get()));for(var field:module().fields())if(field.required()&&(body.path(field.key()).isNull()||(body.path(field.key()).isArray()&&body.path(field.key()).isEmpty())))throw new IllegalArgumentException("Renseignez : "+field.label());var chosen=module();var target=selected;
            if(chosen.id().equals("transfers")&&!confirm("Confirmer la demande de reversement de "+body.path("montant").asText()+" XOF ?"))return;
            execute(()->service.save(chosen,body,target),r->{dirty=false;message.setText("Enregistrement confirmé.");changed.run();if(chosen.id().equals("password")){values.clear();showForm();passwordChanged.run();}else load();});
        }catch(Exception invalid){message.setText(invalid.getMessage());}
    }
    private void buttons(){var m=module();boolean write=m!=null&&m.write()!=null&&service.allowed(m.write())&&service.online();save.setVisible(m!=null&&!m.fields().isEmpty());save.setManaged(save.isVisible());save.setDisable(busy||!write||(m!=null&&!m.singleton()&&selected!=null&&m.updatePath()==null));fresh.setVisible(m!=null&&!m.singleton()&&m.createPath()!=null);fresh.setManaged(fresh.isVisible());fresh.setDisable(busy||!write);remove.setVisible(m!=null&&m.removable());remove.setManaged(remove.isVisible());remove.setDisable(busy||!write||selected==null);modules.setDisable(busy);refresh.setDisable(busy);form.setDisable(busy||!write);extras.setDisable(busy);summary.setDisable(busy);}
    private void changeDate(){
        if(changingDate||busy)return;
        if(date.getValue()==null||(dirty&&!discard())){changingDate=true;date.setValue(loadedDate);changingDate=false;return;}
        dirty=false;load();
    }
    private void programme(){
        date.setId("admin-programme-date");date.setOnAction(e->changeDate());summary.getChildren().add(new HBox(8,new Label("Date"),date));
        JsonNode day=data.isArray()?data.path(0):data;var slots=new VBox(8);var inputs=new ArrayList<Supplier<ObjectNode>>();
        Consumer<JsonNode> add=row->{var hour=new TextField(row.path("heureCelebration").asText("06:30"));hour.setPromptText("06:30");hour.setPrefWidth(85);var label=new TextField(row.path("libelle").asText(""));label.setPromptText("Libellé");var nature=new ComboBox<String>();nature.getItems().addAll("NORMALE","DOMINICALE","SPECIALE");nature.setValue(row.path("natureHonoraire").asText(date.getValue().getDayOfWeek()==DayOfWeek.SUNDAY?"DOMINICALE":"NORMALE"));var line=new HBox(8,hour,label,nature);var delete=new Button("Retirer");Supplier<ObjectNode> value=()->JSON.createObjectNode().put("heureCelebration",LocalTime.parse(hour.getText()).toString()).put("libelle",label.getText()).put("natureHonoraire",nature.getValue());delete.setOnAction(e->{slots.getChildren().remove(line);inputs.remove(value);dirty=true;});line.getChildren().add(delete);inputs.add(value);slots.getChildren().add(line);hour.textProperty().addListener((o,a,b)->changedField());label.textProperty().addListener((o,a,b)->changedField());nature.valueProperty().addListener((o,a,b)->changedField());};
        for(var slot:day.path("creneaux"))add.accept(slot);summary.getChildren().add(slots);
        extra("Ajouter un créneau",service.allowed("schedule:manage"),()->{if(inputs.size()<20){add.accept(JSON.createObjectNode());dirty=true;}});
        extra("Enregistrer cette journée",service.allowed("schedule:manage"),()->{try{var body=JSON.createArrayNode();inputs.forEach(i->body.add(i.get()));if(body.isEmpty())throw new IllegalArgumentException("Prévoyez au moins un créneau.");execute(()->service.programme(date.getValue(),body,false),r->{dirty=false;changed.run();load();});}catch(Exception invalid){message.setText("Vérifiez les heures et les créneaux de la journée.");}});
        extra("Revenir à la grille hebdomadaire",service.allowed("schedule:manage"),()->{if(confirm("Supprimer la personnalisation de cette date ?"))execute(()->service.programme(date.getValue(),JSON.createArrayNode(),true),r->{dirty=false;changed.run();load();});});
    }
    @Override public void close(){closed=true;generation++;worker.shutdownNow();}
}
