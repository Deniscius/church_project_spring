package com.missanye.desktop;

import com.missanye.desktop.controller.WorkbenchController;
import com.missanye.desktop.view.WorkbenchView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import java.nio.file.Path;

public final class DesktopApplication extends Application {
    private WorkbenchController controller;
    @Override public void start(Stage stage) {
        try {
            Path directory = Path.of(System.getProperty("missanye.dataDir", Path.of(System.getProperty("user.home"), ".missanye-desktop").toString()));
            var view = new WorkbenchView(); controller = new WorkbenchController(view, directory.resolve("workflow.sqlite"), getHostServices());
            stage.setTitle("Missanye Desktop — Gestion paroissiale"); stage.setScene(new Scene(view.root, 1100, 720)); stage.show();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir le stockage local. Vérifiez les droits d'accès au dossier de données.").showAndWait();
            javafx.application.Platform.exit();
        }
    }
    @Override public void stop() { if (controller != null) controller.close(); }
    public static void main(String[] args) { launch(args); }
}
