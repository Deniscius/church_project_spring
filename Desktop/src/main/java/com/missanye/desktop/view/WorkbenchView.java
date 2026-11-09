package com.missanye.desktop.view;

import com.missanye.desktop.model.WorkspaceModels.*;
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
    public final TextField server, username, surname, firstname, phone, email, courier, customTime, extraDates;
    public final PasswordField password, localPassword;
    public final Button login, offline, lock, configureSlots, save, fresh, edit, queue, archive, sync, refresh, cash, receipt, checkout, reconcile, correct, validate, remove;
    public final Label session, status, slotsSummary, price, detail, queueDetail;
    public final ComboBox<Choice> parish, type, plan, schedule, payment;
    public final DatePicker date;
    public final CheckBox auto;
    public final ListView<Row> demands, outbox;
    public final TabPane tabs;
    public final VBox workspace;
    private final Map<String,Object> namespace=new HashMap<>();
    private final Region connection;
    private final StackPane lockedPane=new StackPane();
    private boolean workspaceOpen;

    public WorkbenchView() {
        try {
            root=(BorderPane)load("workbench.fxml");
            root.setTop(load("login-form.fxml"));
            TabPane panes=ref("tabs",TabPane.class);
            panes.getTabs().get(0).setContent(load("request-form.fxml"));
            panes.getTabs().get(1).setContent(load("demands.fxml"));
            panes.getTabs().get(2).setContent(load("outbox.fxml"));
            server=ref("server",TextField.class);
            username=ref("username",TextField.class);
            surname=ref("surname",TextField.class);
            firstname=ref("firstname",TextField.class);
            phone=ref("phone",TextField.class);
            email=ref("email",TextField.class);
            courier=ref("courier",TextField.class);
            customTime=ref("customTime",TextField.class);
            extraDates=ref("extraDates",TextField.class);
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
            demands=ref("demands",ListView.class);
            outbox=ref("outbox",ListView.class);
            tabs=ref("tabs",TabPane.class);
            workspace=ref("workspace",VBox.class);
            date.setValue(LocalDate.now().plusDays(2));
            intention=ref("intention",TextArea.class);
            connection=(Region)root.getTop();
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
            root.setCenter(workspace);
            root.setTop(connection);
        } else {
            root.setTop(null);
            root.setCenter(lockedPane);
            lockedPane.getChildren().setAll(connection);
        }
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
