package com.missanye.desktop;

import com.missanye.desktop.controller.WorkbenchController;
import com.missanye.desktop.view.WorkbenchView;
import com.missanye.desktop.service.ApplicationLock;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import java.nio.file.Path;

public final class DesktopApplication extends Application {
    private WorkbenchController controller;
    private ApplicationLock applicationLock;
    @Override public void start(Stage stage) {
        try {
            Path directory = Path.of(System.getProperty("missanye.dataDir", Path.of(System.getProperty("user.home"), ".missanye-desktop").toString()));
            applicationLock=ApplicationLock.acquire(directory);
            var view = new WorkbenchView(); controller = new WorkbenchController(view, directory.resolve("workflow.sqlite"), getHostServices());
            var bounds=javafx.stage.Screen.getPrimary().getVisualBounds();
            stage.getIcons().add(new javafx.scene.image.Image(java.util.Objects.requireNonNull(getClass().getResourceAsStream("branding/missanye.png"))));
            stage.setTitle("Missanye Desktop — Gestion paroissiale");stage.setScene(new Scene(view.root,Math.min(1180,bounds.getWidth()-40),Math.min(820,bounds.getHeight()-40)));stage.setMinWidth(Math.min(960,bounds.getWidth()-40));stage.setMinHeight(Math.min(640,bounds.getHeight()-40)); stage.setOnCloseRequest(event->{event.consume();controller.requestClose(stage::hide);}); stage.show();
        } catch (Exception e) {
            if(applicationLock!=null)try{applicationLock.close();}catch(Exception ignored){}
            new Alert(Alert.AlertType.ERROR, e instanceof ApplicationLock.AlreadyRunningException?e.getMessage():"Impossible de démarrer Missanye. Contactez votre administrateur pour vérifier l'installation.").showAndWait();
            javafx.application.Platform.exit();
        }
    }
    @Override public void stop() { if (controller != null) controller.close(); if(applicationLock!=null)try{applicationLock.close();}catch(Exception ignored){} }
    public static void main(String[] args) { launch(args); }
}
