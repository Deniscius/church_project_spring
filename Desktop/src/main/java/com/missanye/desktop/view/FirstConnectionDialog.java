package com.missanye.desktop.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.missanye.desktop.model.WorkspaceModels.Choice;
import com.missanye.desktop.service.WorkspaceBootstrap;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.*;

/** First-time protection uses a confirmation field; returning users unlock the existing key. */
public final class FirstConnectionDialog {
    private FirstConnectionDialog(){}
    public record Selection(Choice parish,char[] password){}
    public static Optional<Selection> show(WorkbenchView view,JsonNode user,JsonNode parishes,boolean firstProfile){
        try{
            var loader=new FXMLLoader(FirstConnectionDialog.class.getResource("first-connection.fxml"));Parent content=loader.load();var controls=loader.getNamespace();
            @SuppressWarnings("unchecked") ComboBox<Choice> tenant=(ComboBox<Choice>)controls.get("setupParish");
            var secret=(PasswordField)controls.get("setupPassword");var repeated=(PasswordField)controls.get("setupConfirmation");var validation=(Label)controls.get("setupValidation");
            ((Label)controls.get("setupAccount")).setText((user.path("prenom").asText("")+" "+user.path("nom").asText(user.path("username").asText())).trim());
            for(var p:WorkspaceBootstrap.eligible(parishes))tenant.getItems().add(new Choice(WorkspaceBootstrap.parishId(p),p.path("nom").asText(p.path("paroisseNom").asText()),p));
            if(tenant.getItems().size()==1){tenant.getSelectionModel().selectFirst();tenant.setDisable(true);}
            var tenantSection=(VBox)controls.get("setupTenantSection");tenantSection.setVisible(firstProfile);tenantSection.setManaged(firstProfile);
            if(!firstProfile){((Label)controls.get("setupTitle")).setText("Retrouver votre espace sur ce poste");((Label)controls.get("setupPasswordHint")).setText("Saisissez le mot de passe local que vous aviez choisi pour cet ordinateur.");}
            var confirmation=(VBox)controls.get("setupConfirmationSection");confirmation.setVisible(firstProfile);confirmation.setManaged(firstProfile);
            var dialog=view.decorate(new Dialog<Selection>());dialog.setTitle(firstProfile?"Première connexion — Missanye":"Accès au poste — Missanye");dialog.getDialogPane().setContent(content);
            var proceed=new ButtonType(firstProfile?"Préparer mon espace":"Continuer",ButtonBar.ButtonData.OK_DONE);dialog.getDialogPane().getButtonTypes().addAll(new ButtonType("Annuler",ButtonBar.ButtonData.CANCEL_CLOSE),proceed);
            Runnable validate=()->{
                boolean match=!firstProfile||secret.getText().equals(repeated.getText());boolean enough=firstProfile?secret.getText().length()>=12:!secret.getText().isEmpty();
                dialog.getDialogPane().lookupButton(proceed).setDisable(!enough||!match||(firstProfile&&tenant.getValue()==null));
                validation.setText(firstProfile&&!repeated.getText().isEmpty()&&!match?"Les deux mots de passe locaux doivent être identiques.":firstProfile&&secret.getText().length()<12?"Utilisez au moins 12 caractères.":"");
                ((Label)controls.get("setupParishDetails")).setText(tenant.getValue()==null?"":tenant.getValue().data().path("adresse").asText("Accès autorisé à cette paroisse."));
            };
            secret.textProperty().addListener((o,a,b)->validate.run());repeated.textProperty().addListener((o,a,b)->validate.run());tenant.valueProperty().addListener((o,a,b)->validate.run());validate.run();
            dialog.setResultConverter(button->button==proceed?new Selection(firstProfile?tenant.getValue():null,secret.getText().toCharArray()):null);
            try{return dialog.showAndWait();}finally{secret.clear();repeated.clear();}
        }catch(java.io.IOException e){throw new IllegalStateException("Impossible d'ouvrir l'assistant de première connexion.",e);}
    }
}
