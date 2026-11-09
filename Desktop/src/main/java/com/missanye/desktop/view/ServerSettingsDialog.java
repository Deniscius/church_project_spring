package com.missanye.desktop.view;

import com.missanye.desktop.config.*;
import com.missanye.desktop.service.*;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Executors;

public final class ServerSettingsDialog {
    private ServerSettingsDialog(){}
    public static Optional<String> show(WorkbenchView view,Path database,String current){
        var worker=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"server-probe");t.setDaemon(true);return t;});
        try{
            var loader=new FXMLLoader(ServerSettingsDialog.class.getResource("server-settings.fxml"));Parent content=loader.load();var refs=loader.getNamespace();
            var address=(TextField)refs.get("serverAddress");var test=(Button)refs.get("testServer");var result=(Label)refs.get("serverResult");var progress=(ProgressIndicator)refs.get("serverProgress");
            address.setText(current);var dialog=view.decorate(new Dialog<String>());dialog.setTitle("Configuration du serveur — Missanye");dialog.getDialogPane().setContent(content);
            var save=new ButtonType("Enregistrer",ButtonBar.ButtonData.OK_DONE);dialog.getDialogPane().getButtonTypes().addAll(new ButtonType("Annuler",ButtonBar.ButtonData.CANCEL_CLOSE),save);
            var saveButton=(Button)dialog.getDialogPane().lookupButton(save);saveButton.setDisable(true);String[] verified={null};
            address.textProperty().addListener((o,a,b)->{verified[0]=null;saveButton.setDisable(true);result.setText("Testez cette adresse avant d'enregistrer.");});
            test.setOnAction(e->{
                final String candidate;try{candidate=new DesktopConfiguration(address.getText()).apiBaseUrl();}catch(IllegalArgumentException invalid){result.setText(invalid.getMessage());return;}
                verified[0]=null;saveButton.setDisable(true);test.setDisable(true);address.setDisable(true);progress.setVisible(true);progress.setManaged(true);
                result.setText("Connexion en cours… Un serveur en veille peut prendre jusqu'à deux minutes pour démarrer.");
                var task=new Task<Void>(){protected Void call()throws Exception{ServerProbe.test(candidate);return null;}};
                Runnable done=()->{test.setDisable(false);address.setDisable(false);progress.setVisible(false);progress.setManaged(false);};
                task.setOnSucceeded(event->{done.run();verified[0]=candidate;saveButton.setDisable(false);result.setText("Serveur accessible. Vous pouvez enregistrer puis vous connecter avec vos identifiants habituels.");});
                task.setOnFailed(event->{done.run();result.setText(OperationErrors.message(task.getException()));});worker.submit(task);
            });
            saveButton.addEventFilter(javafx.event.ActionEvent.ACTION,e->{
                try{if(verified[0]==null)throw new IllegalArgumentException("Testez d'abord le serveur.");ServerSettings.save(database,verified[0]);}
                catch(Exception failure){e.consume();result.setText("Enregistrement impossible. L'adresse précédente est conservée. Vérifiez les droits d'accès au dossier de l'application.");}
            });
            dialog.setResultConverter(button->button==save?verified[0]:null);return dialog.showAndWait();
        }catch(java.io.IOException e){throw new IllegalStateException("Impossible d'ouvrir la configuration du serveur.",e);}
        finally{worker.shutdownNow();}
    }
}
