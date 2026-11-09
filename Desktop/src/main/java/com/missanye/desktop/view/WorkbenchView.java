package com.missanye.desktop.view;

import com.missanye.desktop.model.WorkspaceModels.*;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.LocalDate;
import java.util.List;

public final class WorkbenchView {
    public final BorderPane root=new BorderPane();
    public final TextField server=new TextField("http://localhost:8081"),username=new TextField();
    public final PasswordField password=new PasswordField(),localPassword=new PasswordField();
    public final Button login=new Button("Connexion en ligne"),offline=new Button("Déverrouiller hors ligne"),lock=new Button("Verrouiller / déconnexion");
    public final Label session=new Label("Coffre verrouillé"),status=new Label("Première connexion : choisir aussi un mot de passe local d'au moins 12 caractères. Ne pas le perdre.");
    public final ComboBox<Choice> parish=new ComboBox<>(),type=new ComboBox<>(),plan=new ComboBox<>(),schedule=new ComboBox<>(),payment=new ComboBox<>();
    public final TextField surname=new TextField(),firstname=new TextField(),phone=new TextField(),email=new TextField(),courier=new TextField(),customTime=new TextField(),extraDates=new TextField();
    public final DatePicker date=new DatePicker(LocalDate.now().plusDays(2));
    public final TextArea intention=new TextArea();
    public final Button configureSlots=new Button("Configurer chaque date et horaire");
    public final Label slotsSummary=new Label("Horaire commun par défaut");
    public final Label price=new Label("Tarif : sélectionner un forfait"),detail=new Label(),queueDetail=new Label();
    public final Button save=new Button("Sauvegarder le brouillon"),fresh=new Button("Nouveau"),edit=new Button("Modifier / corriger"),queue=new Button("Mettre en file d'envoi"),archive=new Button("Archiver"),sync=new Button("Synchroniser"),refresh=new Button("Actualiser catalogue et demandes"),cash=new Button("Enregistrer espèces"),receipt=new Button("Exporter / ouvrir le reçu PDF"),checkout=new Button("Payer en ligne"),reconcile=new Button("Vérifier le paiement");
    public final Button correct=new Button("Corriger l’intention"),validate=new Button("Valider / rejeter"),remove=new Button("Supprimer la demande");
    public final CheckBox auto=new CheckBox("Synchronisation automatique (60 s)");
    public final ListView<Row> demands=new ListView<>(),outbox=new ListView<>();
    public final TabPane tabs=new TabPane();
    public final VBox workspace;
    public WorkbenchView(){
        root.setPadding(new Insets(16));server.setPrefColumnCount(28);username.setPromptText("Identifiant serveur");password.setPromptText("Mot de passe serveur");localPassword.setPromptText("Mot de passe du coffre local");
        var credentials=new VBox(8,new Label("MISSANYE — Gestion paroissiale desktop"),new HBox(8,new Label("API"),server,username),new HBox(8,password,localPassword,login,offline,lock),session);
        root.setTop(credentials);status.setWrapText(true);root.setBottom(status);
        for(var choice:List.of(parish,type,plan,schedule,payment))choice.setMaxWidth(Double.MAX_VALUE);
        auto.setSelected(true);intention.setPrefRowCount(4);extraDates.setPromptText("Dates supplémentaires : 2027-01-03, 2027-01-04…");customTime.setPromptText("HH:mm (sinon horaire du catalogue)");phone.setPromptText("+22890123456");
        var form=new GridPane();form.setHgap(12);form.setVgap(8);int i=0;
        add(form,i++,"Type de demande",type);add(form,i++,"Forfait",plan);add(form,i++,"Tarif indicatif",price);add(form,i++,"Nom",surname);add(form,i++,"Prénom",firstname);add(form,i++,"Téléphone E.164",phone);add(form,i++,"Email (optionnel)",email);add(form,i++,"Coursier (optionnel)",courier);add(form,i++,"Première célébration",date);add(form,i++,"Autres dates du forfait",extraDates);add(form,i++,"Horaire",schedule);add(form,i++,"Heure personnalisée",customTime);add(form,i++,"Créneaux par date",new VBox(4,configureSlots,slotsSummary));add(form,i++,"Mode de paiement",payment);add(form,i++,"Intention (10 à 500 caractères)",intention);
        form.add(new HBox(8,fresh,save),1,i);form.setPadding(new Insets(12));
        var scroll=new ScrollPane(form);scroll.setFitToWidth(true);
        var requests=new VBox(8,new Label("Dernière copie serveur — actualiser pour connaître l'état courant"),demands,detail,new HBox(8,correct,validate,remove),new HBox(8,cash,checkout,reconcile,receipt));detail.setWrapText(true);VBox.setVgrow(demands,Priority.ALWAYS);
        var pending=new VBox(8,new Label("Les espèces restent provisoires jusqu'à confirmation serveur. Aucun paiement n'est validé hors ligne."),outbox,queueDetail,new HBox(8,edit,queue,archive));queueDetail.setWrapText(true);VBox.setVgrow(outbox,Priority.ALWAYS);
        tabs.getTabs().addAll(tab("Nouvelle demande",scroll),tab("Demandes et paiements",requests),tab("Brouillons et synchronisation",pending));
        workspace=new VBox(10,new HBox(8,new Label("Paroisse"),parish,refresh),new HBox(8,sync,auto),tabs);VBox.setVgrow(tabs,Priority.ALWAYS);workspace.setPadding(new Insets(16,0,16,0));root.setCenter(workspace);workspace.setDisable(true);lock.setDisable(true);
        root.setStyle("-fx-font-family: 'SansSerif'; -fx-font-size: 13px;");
    }
    private static void add(GridPane g,int row,String label,javafx.scene.Node node){g.add(new Label(label),0,row);g.add(node,1,row);GridPane.setHgrow(node,Priority.ALWAYS);}
    private static Tab tab(String name,javafx.scene.Node node){var tab=new Tab(name,node);tab.setClosable(false);return tab;}
}
