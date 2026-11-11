package com.missanye.desktop.view;
import com.missanye.desktop.api.ApiClient;
import com.missanye.desktop.config.*;
import com.missanye.desktop.model.WorkspaceModels.Choice;
import com.missanye.desktop.service.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.concurrent.Task;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
public final class ParishDeploymentDialog{
 private ParishDeploymentDialog(){}
 public static Optional<ParishDeployment> show(WorkbenchView view,Path file,String current){
  var worker=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"parish-deployment");t.setDaemon(true);return t;});
  try{
   var loader=new FXMLLoader(ParishDeploymentDialog.class.getResource("parish-deployment.fxml"));Parent content=loader.load();var n=loader.getNamespace();
   var server=(TextField)n.get("deploymentServer");var user=(TextField)n.get("deploymentUser");var password=(PasswordField)n.get("deploymentPassword");var verify=(Button)n.get("deploymentVerify");var result=(Label)n.get("deploymentResult");var progress=(ProgressIndicator)n.get("deploymentProgress");
   @SuppressWarnings("unchecked") ComboBox<Choice> parishes=(ComboBox<Choice>)n.get("deploymentParish");server.setText(current);
   var dialog=view.decorate(new Dialog<ParishDeployment>());dialog.setTitle("Déploiement paroissial — Missanye");dialog.getDialogPane().setContent(content);var save=new ButtonType("Rattacher ce poste",ButtonBar.ButtonData.OK_DONE);dialog.getDialogPane().getButtonTypes().addAll(new ButtonType("Annuler",ButtonBar.ButtonData.CANCEL_CLOSE),save);var accept=(Button)dialog.getDialogPane().lookupButton(save);accept.setDisable(true);String[] checked={null};
   Runnable invalidate=()->{checked[0]=null;parishes.getItems().clear();parishes.setDisable(true);accept.setDisable(true);};server.textProperty().addListener((o,a,b)->invalidate.run());user.textProperty().addListener((o,a,b)->invalidate.run());password.textProperty().addListener((o,a,b)->invalidate.run());parishes.valueProperty().addListener((o,a,b)->accept.setDisable(checked[0]==null||b==null));
   verify.setOnAction(e->{
    final String address;try{address=new DesktopConfiguration(server.getText()).apiBaseUrl();if(user.getText().isBlank()||password.getText().isEmpty())throw new IllegalArgumentException("Saisissez vos identifiants habituels.");}catch(Exception invalid){result.setText(invalid.getMessage());return;}
    String username=user.getText().strip(),secret=password.getText();invalidate.run();server.setDisable(true);user.setDisable(true);password.setDisable(true);verify.setDisable(true);progress.setVisible(true);progress.setManaged(true);result.setText("Vérification du serveur et du compte… Patientez jusqu'à deux minutes.");
    var task=new Task<List<Choice>>(){protected List<Choice> call()throws Exception{ServerProbe.test(address);var api=new ApiClient(address);try{var session=api.login(username,secret);var accessible=session.path("user").path("isGlobal").asBoolean()?api.get("paroisses"):session.path("paroisses");var choices=new ArrayList<Choice>();for(var p:WorkspaceBootstrap.eligible(accessible))choices.add(new Choice(WorkspaceBootstrap.parishId(p),p.path("nom").asText(),p));return choices;}finally{api.clear();}}};
    Runnable done=()->{server.setDisable(false);user.setDisable(false);password.setDisable(false);verify.setDisable(false);progress.setVisible(false);progress.setManaged(false);};
    task.setOnSucceeded(event->{done.run();password.clear();checked[0]=address;parishes.getItems().setAll(task.getValue());parishes.setDisable(false);if(parishes.getItems().size()==1)parishes.getSelectionModel().selectFirst();result.setText(parishes.getItems().isEmpty()?"Compte vérifié. Créez une paroisse active depuis le web avant le déploiement.":"✓ Compte vérifié. Sélectionnez la paroisse à rattacher à ce poste.");});task.setOnFailed(event->{done.run();password.clear();result.setText(OperationErrors.message(task.getException()));});worker.submit(task);
   });
   ParishDeployment[] selected={null};accept.addEventFilter(javafx.event.ActionEvent.ACTION,event->{try{var p=parishes.getValue();if(checked[0]==null||p==null)throw new IllegalArgumentException();selected[0]=new ParishDeployment(checked[0],p.id(),p.label());ServerSettings.saveDeployment(file,selected[0]);}catch(Exception failed){event.consume();result.setText("Le rattachement n'a pas pu être enregistré. Vérifiez le dossier de configuration.");}});dialog.setResultConverter(button->button==save?selected[0]:null);try{return dialog.showAndWait();}finally{password.clear();}
  }catch(java.io.IOException error){throw new IllegalStateException("Impossible d'ouvrir le déploiement du poste.",error);}finally{worker.shutdownNow();}
 }
}
