package com.missanye.desktop.view;

import com.missanye.desktop.model.WorkspaceModels.*;
import com.missanye.desktop.model.ScreenFormats;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.beans.property.ReadOnlyStringWrapper;
import java.util.function.Function;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

/** Loads declarative FXML views. Business actions are wired by WorkbenchController. */
public final class WorkbenchView {
    public final BorderPane root;
    public final TextArea intention;
    public final TextField demandSearch, username, surname, firstname, phone, email, courier, customTime;
    public final PasswordField password, localPassword;
    public final Button login, offline, lock, configureSlots, save, fresh, edit, queue, archive, sync, refresh, cash, receipt, checkout, reconcile, correct, validate, remove;
    public final Label session, status, slotsSummary, price, detail, queueDetail;
    public final ComboBox<Choice> parish, type, plan, schedule, payment;
    public final DatePicker date, additionalDate;
    public final ListView<LocalDate> extraDates;
    public final Button addDate,removeDate;
    public final CheckBox auto;
    public final TableView<Row> demands, outbox;
    public final ComboBox<String> queueFilter;
    public final ObservableList<Row> demandRows=FXCollections.observableArrayList(), activityRows=FXCollections.observableArrayList();
    public final ProgressIndicator busyIndicator;
    public final Label demandCount,pendingCount,rejectedCount;
    public final TabPane tabs;
    public final VBox workspace;
    private final Map<String,Object> namespace=new HashMap<>();
    private final Region connection,header,workspaceContainer;
    private final StackPane lockedPane=new StackPane();
    private boolean workspaceOpen;

    public WorkbenchView() {
        try {
            root=(BorderPane)load("workbench.fxml");
            header=(Region)root.getTop();workspaceContainer=(Region)root.getCenter();
            connection=(Region)load("login-form.fxml");
            TabPane panes=ref("tabs",TabPane.class);
            panes.getTabs().get(0).setContent(load("request-form.fxml"));
            panes.getTabs().get(1).setContent(load("demands.fxml"));
            panes.getTabs().get(2).setContent(load("outbox.fxml"));
            demandSearch=ref("demandSearch",TextField.class);
            username=ref("username",TextField.class);
            surname=ref("surname",TextField.class);
            firstname=ref("firstname",TextField.class);
            phone=ref("phone",TextField.class);
            email=ref("email",TextField.class);
            courier=ref("courier",TextField.class);
            customTime=ref("customTime",TextField.class);
            extraDates=ref("extraDates",ListView.class);
            additionalDate=ref("additionalDate",DatePicker.class);addDate=ref("addDate",Button.class);removeDate=ref("removeDate",Button.class);
            password=ref("password",PasswordField.class);
            localPassword=ref("localPassword",PasswordField.class);
            login=ref("login",Button.class);
            offline=ref("offline",Button.class);
            lock=ref("lock",Button.class);
            configureSlots=ref("configureSlots",Button.class);
            save=ref("save",Button.class);
            fresh=ref("fresh",Button.class);
            edit=ref("edit",Button.class);
            queue=ref("queue",Button.class);
            archive=ref("archive",Button.class);
            sync=ref("sync",Button.class);
            refresh=ref("refresh",Button.class);
            cash=ref("cash",Button.class);
            receipt=ref("receipt",Button.class);
            checkout=ref("checkout",Button.class);
            reconcile=ref("reconcile",Button.class);
            correct=ref("correct",Button.class);
            validate=ref("validate",Button.class);
            remove=ref("remove",Button.class);
            session=ref("session",Label.class);
            status=ref("status",Label.class);
            slotsSummary=ref("slotsSummary",Label.class);
            price=ref("price",Label.class);
            detail=ref("detail",Label.class);
            queueDetail=ref("queueDetail",Label.class);
            parish=ref("parish",ComboBox.class);
            type=ref("type",ComboBox.class);
            plan=ref("plan",ComboBox.class);
            schedule=ref("schedule",ComboBox.class);
            payment=ref("payment",ComboBox.class);
            date=ref("date",DatePicker.class);
            auto=ref("auto",CheckBox.class);
            demands=ref("demands",TableView.class);
            outbox=ref("outbox",TableView.class);
            tabs=ref("tabs",TabPane.class);
            workspace=ref("workspace",VBox.class);
            date.setValue(LocalDate.now().plusDays(2));additionalDate.setValue(date.getValue().plusDays(1));
            extraDates.setCellFactory(list->new ListCell<>(){@Override protected void updateItem(LocalDate date,boolean empty){super.updateItem(date,empty);setText(empty||date==null?null:date.format(java.time.format.DateTimeFormatter.ofPattern("EEEE d MMMM yyyy",Locale.FRANCE)));}});
            intention=ref("intention",TextArea.class);
            queueFilter=ref("queueFilter",ComboBox.class);
            busyIndicator=ref("busyIndicator",ProgressIndicator.class);
            demandCount=ref("demandCount",Label.class);pendingCount=ref("pendingCount",Label.class);rejectedCount=ref("rejectedCount",Label.class);
            configureTables();
            tabs.minHeightProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(()->{
                var selected=tabs.getSelectionModel().getSelectedItem();
                if(selected==null || !(selected.getContent() instanceof Region content))return 360.0;
                return Math.max(360,content.minHeight(Math.max(300,tabs.getWidth()-32))+52);
            },tabs.widthProperty(),tabs.getSelectionModel().selectedItemProperty(),detail.textProperty(),queueDetail.textProperty()));
            connection.setMaxHeight(Region.USE_PREF_SIZE);
            root.setTop(null);
            lockedPane.getChildren().add(connection);
            root.setCenter(lockedPane);
        } catch(IOException e) {
            throw new IllegalStateException("Impossible de charger les vues FXML du desktop.",e);
        }
    }
    public void setWorkspaceOpen(boolean open) {
        if(open==workspaceOpen)return;
        workspaceOpen=open;
        if(open) {
            lockedPane.getChildren().clear();
            root.setCenter(workspaceContainer);
            root.setTop(header);
        } else {
            root.setTop(null);
            root.setCenter(lockedPane);
            lockedPane.getChildren().setAll(connection);
        }
    }
    private void configureTables() {
        var visibleDemands=new FilteredList<>(demandRows);
        demands.setItems(visibleDemands);
        demandSearch.textProperty().addListener((o,a,b)->visibleDemands.setPredicate(row->b==null || b.isBlank() || row.data().toString().toLowerCase(Locale.ROOT).contains(b.trim().toLowerCase(Locale.ROOT))));
        var visibleActivity=new FilteredList<>(activityRows);
        outbox.setItems(visibleActivity);
        queueFilter.getItems().setAll("Toutes les opérations","Brouillons","À transmettre","Confirmées","À corriger");queueFilter.getSelectionModel().selectFirst();
        queueFilter.valueProperty().addListener((o,a,b)->visibleActivity.setPredicate(row->switch(b){case "Brouillons"->"DRAFT".equals(row.data().path("state").asText());case "À transmettre"->"PENDING".equals(row.data().path("state").asText());case "Confirmées"->"SENT".equals(row.data().path("state").asText());case "À corriger"->"REJECTED".equals(row.data().path("state").asText());default->true;}));
        column("demandReference",d->d.path("codeSuivie").asText());
        column("demandFaithful",ScreenFormats::faithful);
        column("demandDate",d->ScreenFormats.date(d.path("datesCelebration").path(0).asText()));
        column("demandAmount",d->ScreenFormats.money(d.path("montant")));
        column("demandPayment",d->ScreenFormats.state(d.path("statutPaiement").asText()));
        column("demandValidation",d->ScreenFormats.state(d.path("statutValidation").asText()));
        column("activityDate",d->ScreenFormats.date(d.path("createdAt").asText()));
        column("activityKind",d->ScreenFormats.kind(d.path("kind").asText()));
        column("activityReference",ScreenFormats::reference);
        column("activityIntention",d->d.path("payload").path("intention").asText(d.path("result").path("intention").asText("—")));
        column("activityState",d->ScreenFormats.state(d.path("state").asText()));
        demands.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);outbox.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        demandRows.addListener((javafx.collections.ListChangeListener<Row>)c->demandCount.setText(Integer.toString(demandRows.size())));
        activityRows.addListener((javafx.collections.ListChangeListener<Row>)c->{pendingCount.setText(Long.toString(activityRows.stream().filter(r->"PENDING".equals(r.data().path("state").asText())).count()));rejectedCount.setText(Long.toString(activityRows.stream().filter(r->"REJECTED".equals(r.data().path("state").asText())).count()));});
    }
    @SuppressWarnings("unchecked")
    private void column(String id,Function<JsonNode,String> text) {
        TableColumn<Row,String> column=ref(id,TableColumn.class);
        column.setCellValueFactory(cell->new ReadOnlyStringWrapper(text.apply(cell.getValue().data())));
        if(Set.of("demandPayment","demandValidation","activityState").contains(id))column.setCellFactory(c->new TableCell<>(){
            @Override protected void updateItem(String value,boolean empty){super.updateItem(value,empty);setText(null);if(empty||value==null){setGraphic(null);return;}
                var badge=new Label(value);badge.getStyleClass().add("status-pill");badge.getStyleClass().add(Set.of("Payé","Validée","Confirmée").contains(value)?"status-success":Set.of("À corriger","Rejetée","Annulée").contains(value)?"status-danger":"status-neutral");setGraphic(badge);
            }
        });
    }
    public <T> Dialog<T> decorate(Dialog<T> dialog) {
        if(root.getScene()!=null)dialog.initOwner(root.getScene().getWindow());
        dialog.getDialogPane().getStylesheets().add(Objects.requireNonNull(getClass().getResource("desktop.css")).toExternalForm());
        dialog.getDialogPane().getStyleClass().add("app-dialog");return dialog;
    }
    private Parent load(String filename) throws IOException {
        var location=Objects.requireNonNull(WorkbenchView.class.getResource(filename),"Ressource FXML absente : "+filename);
        var loader=new FXMLLoader(location);
        Parent view=loader.load();
        namespace.putAll(loader.getNamespace());
        return view;
    }
    @SuppressWarnings("unchecked")
    private <T> T ref(String id,Class<?> type) {
        Object value=namespace.get(id);
        if(!type.isInstance(value))throw new IllegalStateException("Composant FXML manquant ou incompatible : "+id);
        return (T)value;
    }
}
