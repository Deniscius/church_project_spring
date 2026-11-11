package com.missanye.desktop.view;

import com.missanye.desktop.model.WorkspaceModels.*;
import com.missanye.desktop.model.ScreenFormats;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
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
    public final Button finalizeRequest, configureServer, returnToDraft, retry, login, offline, lock, configureSlots, save, fresh, edit, queue, archive, sync, refresh, cash, receipt, checkout, reconcile, correct, validate, remove;
    public final Label serverEnvironment, loginStatus, offlineAvailability, session, status, slotsSummary, price, detail, queueDetail;
    public final ComboBox<Choice> parish, type, plan, schedule, payment;
    public final DatePicker date, additionalDate;
    public final ListView<LocalDate> extraDates;
    public final Button addDate,removeDate;
    public final CheckBox auto;
    public final TitledPane offlinePane;
    public final TableView<Row> demands, outbox;
    public final ComboBox<String> queueFilter;
    public final ObservableList<Row> demandRows=FXCollections.observableArrayList(), activityRows=FXCollections.observableArrayList();
    public final ProgressIndicator busyIndicator;
    public final Label demandCount,pendingCount,rejectedCount;
    public final TabPane tabs;
    public final VBox workspace;
    private final Map<String,Object> namespace=new HashMap<>();
    private final Region connection,header,workspaceContainer,sidebarContainer,footer;
    private final StackPane lockedPane=new StackPane();
    private boolean workspaceOpen;
    private final javafx.beans.property.ReadOnlyBooleanWrapper opened=new javafx.beans.property.ReadOnlyBooleanWrapper(false);
    public javafx.beans.property.ReadOnlyBooleanProperty workspaceOpenProperty(){return opened.getReadOnlyProperty();}
    private int openDialogs;
    public boolean hasOpenDialog(){return openDialogs>0;}

    public WorkbenchView() {
        try {
            root=(BorderPane)load("workbench.fxml");
            header=(Region)root.getTop();workspaceContainer=(Region)root.getCenter();sidebarContainer=(Region)root.getLeft();footer=(Region)root.getBottom();
            var loginCard=(Region)load("login-form.fxml");loginCard.setMaxHeight(Double.MAX_VALUE);loginCard.setMinHeight(Region.USE_PREF_SIZE);loginCard.setMaxWidth(Double.MAX_VALUE);
            loginCard.setMinWidth(0);
            if(loginCard instanceof GridPane grid){
                var brand=grid.getChildren().get(0);grid.widthProperty().addListener((o,a,b)->{
                    boolean compact=b.doubleValue()<760;brand.setVisible(!compact);brand.setManaged(!compact);grid.getColumnConstraints().get(0).setPercentWidth(compact?0:43);grid.getColumnConstraints().get(1).setPercentWidth(compact?100:57);grid.setHgap(0);
                });
            }
            var loginHolder=new StackPane(loginCard);loginHolder.getStyleClass().add("login-surface");loginHolder.setMinHeight(Region.USE_PREF_SIZE);
            var loginScroll=new ScrollPane(loginHolder);loginScroll.setFitToWidth(true);loginScroll.setFitToHeight(true);loginScroll.getStyleClass().add("workspace-scroll");connection=loginScroll;
            TabPane panes=ref("tabs",TabPane.class);
            panes.getTabs().get(0).setContent(load("request-form.fxml"));
            panes.getTabs().get(1).setContent(load("demands.fxml"));
            panes.getTabs().get(2).setContent(load("outbox.fxml"));
            panes.getTabs().get(3).setContent(load("overview.fxml"));
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
            archive=ref("archive",Button.class);retry=ref("retry",Button.class);returnToDraft=ref("returnToDraft",Button.class);
            sync=ref("sync",Button.class);
            refresh=ref("refresh",Button.class);
            cash=ref("cash",Button.class);
            receipt=ref("receipt",Button.class);
            checkout=ref("checkout",Button.class);
            reconcile=ref("reconcile",Button.class);
            correct=ref("correct",Button.class);
            validate=ref("validate",Button.class);
            remove=ref("remove",Button.class);
            finalizeRequest=ref("finalizeRequest",Button.class);configureServer=ref("configureServer",Button.class);offlinePane=ref("offlinePane",TitledPane.class);offlineAvailability=ref("offlineAvailability",Label.class);
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
            date.setValue(LocalDate.now().plusDays(2));date.getEditor().setText(date.getConverter().toString(date.getValue()));additionalDate.setValue(date.getValue().plusDays(1));
            extraDates.setCellFactory(list->new ListCell<>(){@Override protected void updateItem(LocalDate date,boolean empty){super.updateItem(date,empty);setText(empty||date==null?null:date.format(java.time.format.DateTimeFormatter.ofPattern("EEEE d MMMM yyyy",Locale.FRANCE)));}});
            intention=ref("intention",TextArea.class);
            queueFilter=ref("queueFilter",ComboBox.class);
            busyIndicator=ref("busyIndicator",ProgressIndicator.class);
            demandCount=ref("demandCount",Label.class);pendingCount=ref("pendingCount",Label.class);rejectedCount=ref("rejectedCount",Label.class);
            configureTables();configureNavigation();
            tabs.minHeightProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(()->{
                var selected=tabs.getSelectionModel().getSelectedItem();
                if(selected==null || !(selected.getContent() instanceof Region content))return 360.0;
                return Math.max(360,content.minHeight(Math.max(300,tabs.getWidth()-32))+52);
            },tabs.widthProperty(),tabs.getSelectionModel().selectedItemProperty(),detail.textProperty(),queueDetail.textProperty()));
            loginStatus=ref("loginStatus",Label.class);serverEnvironment=ref("serverEnvironment",Label.class);loginStatus.textProperty().bind(status.textProperty());
            root.setTop(null);root.setLeft(null);root.setBottom(null);
            lockedPane.getChildren().add(connection);
            root.setCenter(lockedPane);
        } catch(IOException e) {
            throw new IllegalStateException("Impossible de charger les vues FXML du desktop.",e);
        }
    }
    public void showPreparation(String parish){
        try{var loader=new FXMLLoader(getClass().getResource("preparation.fxml"));Parent content=loader.load();((Label)loader.getNamespace().get("preparationParish")).setText(parish);lockedPane.getChildren().setAll(content);}
        catch(IOException e){throw new IllegalStateException("Impossible d'afficher la préparation du poste.",e);}
    }
    public void showLogin(){if(!workspaceOpen)lockedPane.getChildren().setAll(connection);}
    public void setWorkspaceOpen(boolean open) {
        if(open==workspaceOpen)return;
        workspaceOpen=open;
        if(open) {
            lockedPane.getChildren().clear();
            root.setCenter(workspaceContainer);
            root.setTop(header);root.setLeft(sidebarContainer);root.setBottom(footer);tabs.getSelectionModel().select(3);
        } else {
            root.setTop(null);root.setLeft(null);root.setBottom(null);
            root.setCenter(lockedPane);
            lockedPane.getChildren().setAll(connection);
        }
        opened.set(open);
    }
    public void parishIdentity(String name){var label=ref("parishIdentity",Label.class);label.setText(name);label.setVisible(true);label.setManaged(true);parish.setVisible(false);parish.setManaged(false);}
    public boolean actionVisible(String id){return ref(id,javafx.scene.Node.class).isVisible();}
    public void showAction(String id,boolean visible){var node=ref(id,javafx.scene.Node.class);node.setVisible(visible);node.setManaged(visible);}
    private void configureNavigation(){
        sidebarContainer.disableProperty().bind(workspace.disableProperty());
        var title=ref("pageTitle",Label.class);var overviewParish=ref("overviewParish",Label.class);
        parish.valueProperty().addListener((o,a,b)->overviewParish.setText(b==null?"Votre paroisse":b.label()));
        var navigation=List.of(ref("navRequest",Button.class),ref("navDemands",Button.class),ref("navActivity",Button.class),ref("navHome",Button.class));
        var labels=List.of("Nouvelle demande","Demandes et paiements","Activité paroissiale","Tableau de bord");
        for(int i=0;i<navigation.size();i++){final int index=i;var button=navigation.get(i);button.setTooltip(new Tooltip(labels.get(i)));button.setOnAction(e->tabs.getSelectionModel().select(index));}
        ref("homeRequest",Button.class).setOnAction(e->tabs.getSelectionModel().select(0));ref("homeDemands",Button.class).setOnAction(e->tabs.getSelectionModel().select(1));ref("homeActivity",Button.class).setOnAction(e->tabs.getSelectionModel().select(2));
        tabs.getSelectionModel().selectedIndexProperty().addListener((o,a,b)->{int index=b.intValue();if(index<0)return;title.setText(labels.get(index));((ScrollPane)workspaceContainer).setVvalue(0);for(int i=0;i<navigation.size();i++){var classes=navigation.get(i).getStyleClass();classes.remove("nav-active");if(i==index)classes.add("nav-active");}});
        root.widthProperty().addListener((o,a,b)->{
            boolean compact=b.doubleValue()<1180;sidebarContainer.setPrefWidth(compact?76:228);
            ref("sidebarBrand",Label.class).setText("MISSANYE");ref("sidebarBrand",Label.class).setVisible(!compact);ref("sidebarBrand",Label.class).setManaged(!compact);
            for(String id:List.of("sidebarCaption","sidebarHelp")){var node=ref(id,Region.class);node.setVisible(!compact);node.setManaged(!compact);}
            for(int i=0;i<navigation.size();i++)navigation.get(i).setText(compact?"":labels.get(i));
        });
        root.heightProperty().addListener((o,a,b)->{root.getStyleClass().remove("compact-height");if(b.doubleValue()<800)root.getStyleClass().add("compact-height");});
        var tiles=ref("summaryTiles",TilePane.class);tiles.prefTileWidthProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(()->{double width=tiles.getWidth();int columns=width>=680?3:width>=440?2:1;return Math.max(160,Math.floor((width-(columns-1)*14-4)/columns));},tiles.widthProperty()));
        @SuppressWarnings("unchecked") ListView<Row> recent=ref("recentDemands",ListView.class);
        recent.setCellFactory(list->new ListCell<>(){@Override protected void updateItem(Row row,boolean empty){super.updateItem(row,empty);setText(null);setGraphic(null);if(empty||row==null)return;var reference=new Label(ScreenFormats.reference(row.data()));reference.getStyleClass().add("field-label");var intention=new Label(row.data().path("intention").asText("Intention de messe"));intention.setWrapText(true);intention.setMaxWidth(Double.MAX_VALUE);var box=new VBox(4,reference,intention);box.setMinWidth(0);box.prefWidthProperty().bind(javafx.beans.binding.Bindings.max(100,recent.widthProperty().subtract(50)));setGraphic(box);}});
        demandRows.addListener((javafx.collections.ListChangeListener<Row>)change->recent.getItems().setAll(demandRows.subList(0,Math.min(5,demandRows.size()))));
        recent.setOnMouseClicked(e->{if(e.getClickCount()==2&&recent.getSelectionModel().getSelectedItem()!=null){demandSearch.clear();tabs.getSelectionModel().select(1);demands.getSelectionModel().select(recent.getSelectionModel().getSelectedItem());}});
        recent.setOnKeyPressed(e->{if(e.getCode()==javafx.scene.input.KeyCode.ENTER&&recent.getSelectionModel().getSelectedItem()!=null){demandSearch.clear();tabs.getSelectionModel().select(1);demands.getSelectionModel().select(recent.getSelectionModel().getSelectedItem());}});
    }
    private void configureTables() {
        var visibleDemands=new FilteredList<>(demandRows);
        var sortedDemands=new SortedList<>(visibleDemands);sortedDemands.comparatorProperty().bind(demands.comparatorProperty());demands.setItems(sortedDemands);
        demandSearch.textProperty().addListener((o,a,b)->visibleDemands.setPredicate(row->b==null || b.isBlank() || row.data().toString().toLowerCase(Locale.ROOT).contains(b.trim().toLowerCase(Locale.ROOT))));
        var visibleActivity=new FilteredList<>(activityRows);
        var sortedActivity=new SortedList<>(visibleActivity);sortedActivity.comparatorProperty().bind(outbox.comparatorProperty());outbox.setItems(sortedActivity);
        queueFilter.getItems().setAll("Toutes les opérations","Brouillons","Enregistrées","Confirmées","À corriger","Bloquées");queueFilter.getSelectionModel().selectFirst();
        queueFilter.valueProperty().addListener((o,a,b)->visibleActivity.setPredicate(row->switch(b){case "Brouillons"->"DRAFT".equals(row.data().path("state").asText());case "Enregistrées"->"PENDING".equals(row.data().path("state").asText());case "Confirmées"->"SENT".equals(row.data().path("state").asText());case "À corriger"->"REJECTED".equals(row.data().path("state").asText());case "Bloquées"->"BLOCKED".equals(row.data().path("state").asText());default->true;}));
        column("demandReference",ScreenFormats::reference);
        column("demandFaithful",ScreenFormats::faithful);
        column("demandDate",d->ScreenFormats.date(d.path("datesCelebration").path(0).asText()));
        column("demandAmount",d->ScreenFormats.money(d.path("montant")));
        column("demandPayment",d->ScreenFormats.state(d.path("statutPaiement").asText()));
        column("demandValidation",d->d.path("localReconciliationRequired").asBoolean()?"À rapprocher":ScreenFormats.state(d.path("statutValidation").asText()));
        column("activityDate",d->ScreenFormats.date(d.path("createdAt").asText()));
        column("activityKind",d->ScreenFormats.kind(d.path("kind").asText()));
        column("activityReference",ScreenFormats::reference);
        column("activityIntention",d->d.path("payload").path("intention").asText(d.path("result").path("intention").asText("—")));
        column("activityState",d->ScreenFormats.state(d.path("state").asText()));
        for(var table:List.of(demands,outbox)){
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            table.widthProperty().addListener((o,a,b)->table.setColumnResizePolicy(b.doubleValue()<780?TableView.UNCONSTRAINED_RESIZE_POLICY:TableView.CONSTRAINED_RESIZE_POLICY));
        }
        demandRows.addListener((javafx.collections.ListChangeListener<Row>)c->demandCount.setText(Integer.toString(demandRows.size())));
        activityRows.addListener((javafx.collections.ListChangeListener<Row>)c->{pendingCount.setText(Long.toString(activityRows.stream().filter(r->"PENDING".equals(r.data().path("state").asText())).count()));rejectedCount.setText(Long.toString(activityRows.stream().filter(r->Set.of("REJECTED","BLOCKED").contains(r.data().path("state").asText())).count()));});
    }
    @SuppressWarnings("unchecked")
    private void column(String id,Function<JsonNode,String> text) {
        TableColumn<Row,String> column=ref(id,TableColumn.class);
        column.setCellValueFactory(cell->new ReadOnlyStringWrapper(text.apply(cell.getValue().data())));
        if(id.equals("demandAmount"))column.setComparator(Comparator.comparing(value->{String digits=value.replaceAll("[^0-9]","");return digits.isEmpty()?java.math.BigInteger.ZERO:new java.math.BigInteger(digits);}));
        if(Set.of("demandDate","activityDate").contains(id))column.setComparator(Comparator.comparing(value->{try{return LocalDate.parse(value,java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu"));}catch(Exception e){return LocalDate.MIN;}}));
        if(Set.of("demandPayment","demandValidation","activityState").contains(id))column.setCellFactory(c->new TableCell<>(){
            @Override protected void updateItem(String value,boolean empty){super.updateItem(value,empty);setText(null);if(empty||value==null){setGraphic(null);return;}
                var badge=new Label(value);badge.getStyleClass().add("status-pill");badge.getStyleClass().add(Set.of("Payé","Validée","Confirmée").contains(value)?"status-success":Set.of("À corriger","Rejetée","Annulée").contains(value)?"status-danger":"status-neutral");setGraphic(badge);
            }
        });
    }
    public <T> Dialog<T> decorate(Dialog<T> dialog) {
        if(root.getScene()!=null)dialog.initOwner(root.getScene().getWindow());
        var pane=dialog.getDialogPane();pane.getStylesheets().add(Objects.requireNonNull(getClass().getResource("desktop.css")).toExternalForm());
        pane.getStyleClass().add("app-dialog");dialog.setResizable(true);
        dialog.setOnShowing(e->{
            openDialogs++;
            for(var type:pane.getButtonTypes())if(pane.lookupButton(type) instanceof Button button){if(type==ButtonType.CANCEL)button.setText("Annuler");else if(type==ButtonType.OK)button.setText("Confirmer");}

            var screen=javafx.stage.Screen.getPrimary();var owner=dialog.getOwner();
            if(owner!=null){var screens=javafx.stage.Screen.getScreensForRectangle(owner.getX(),owner.getY(),owner.getWidth(),owner.getHeight());if(!screens.isEmpty())screen=screens.get(0);}
            var bounds=screen.getVisualBounds();double maxWidth=Math.max(280,bounds.getWidth()-48),maxHeight=Math.max(240,bounds.getHeight()-48);
            double width=Math.min(660,maxWidth);pane.setPrefWidth(width);pane.setMaxWidth(maxWidth);pane.setMaxHeight(maxHeight);
            var content=pane.getContent();
            if(content!=null){
                wrapDialogLabels(content);
                ScrollPane scroll;
                if(content instanceof ScrollPane existing)scroll=existing;
                else {scroll=new ScrollPane(content);pane.setContent(scroll);}
                scroll.setFitToWidth(true);scroll.getStyleClass().add("dialog-scroll");scroll.setMinHeight(0);
                var inner=scroll.getContent();
                if(inner instanceof Region region){region.setMinWidth(0);region.setMaxWidth(Double.MAX_VALUE);}
                inner.applyCss();double natural=(inner instanceof Region region?region.prefHeight(Math.max(200,width-65)):inner.getLayoutBounds().getHeight());
                scroll.setPrefViewportHeight(Math.min(Math.max(100,natural),Math.max(120,maxHeight-170)));

            }
        });
        dialog.setOnShown(e->{
            pane.applyCss();pane.layout();
            var window=pane.getScene().getWindow();var screens=javafx.stage.Screen.getScreensForRectangle(window.getX(),window.getY(),window.getWidth(),window.getHeight());
            var bounds=(screens.isEmpty()?javafx.stage.Screen.getPrimary():screens.get(0)).getVisualBounds();double limit=Math.max(240,bounds.getHeight()-48);
            if(pane.getContent() instanceof ScrollPane scroll){
                var inner=scroll.getContent();double width=Math.max(200,pane.getPrefWidth()-65);
                double natural=inner instanceof Region region?region.prefHeight(width):inner.getLayoutBounds().getHeight();
                scroll.setPrefViewportHeight(Math.min(Math.max(100,natural),Math.max(120,limit-170)));
            }
            if(window instanceof javafx.stage.Stage stage){stage.setMinWidth(Math.min(420,bounds.getWidth()-48));stage.setMinHeight(Math.min(250,limit));stage.setMaxWidth(bounds.getWidth()-48);stage.setMaxHeight(limit);}
            window.sizeToScene();window.setX(Math.max(bounds.getMinX(),Math.min(window.getX(),bounds.getMaxX()-window.getWidth())));window.setY(Math.max(bounds.getMinY(),Math.min(window.getY(),bounds.getMaxY()-window.getHeight())));
        });
        dialog.setOnHidden(e->openDialogs=Math.max(0,openDialogs-1));return dialog;
    }
    private void wrapDialogLabels(javafx.scene.Node node){
        if(node instanceof Label label){label.setWrapText(true);label.setMinHeight(Region.USE_PREF_SIZE);label.setMaxWidth(Double.MAX_VALUE);}
        if(node instanceof Parent parent)for(var child:parent.getChildrenUnmodifiable())wrapDialogLabels(child);
    }
    private Parent load(String filename) throws IOException {
        var location=Objects.requireNonNull(WorkbenchView.class.getResource(filename),"Ressource FXML absente : "+filename);
        var loader=new FXMLLoader(location);
        Parent view=loader.load();
        namespace.putAll(loader.getNamespace());
        return view;
    }
    @SuppressWarnings("unchecked")
    private <T> T ref(String id,Class<T> type) {
        Object value=namespace.get(id);
        if(!type.isInstance(value))throw new IllegalStateException("Composant FXML manquant ou incompatible : "+id);
        return (T)value;
    }
}
