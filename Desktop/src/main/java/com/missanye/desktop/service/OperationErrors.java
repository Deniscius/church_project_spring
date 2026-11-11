package com.missanye.desktop.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.missanye.desktop.api.ApiClient;
import javax.net.ssl.SSLException;
import java.net.*;
import java.net.http.HttpTimeoutException;
import java.nio.channels.UnresolvedAddressException;
import java.util.*;

/** Explain recoverable failures without exposing request bodies, credentials or server internals. */
public final class OperationErrors {
    private OperationErrors(){}
    public static String message(Throwable failure){
        if(failure instanceof ParishLoginService.PreparationUnavailable)return failure.getMessage();
        if(failure instanceof ApiClient.ApiException response&&response.status()>=500)return response.getMessage()+" (HTTP "+response.status()+")";
        if(failure instanceof ApiClient.ApiException||failure instanceof IllegalArgumentException)return failure.getMessage();
        var causes=new ArrayList<Throwable>();var seen=Collections.newSetFromMap(new IdentityHashMap<Throwable,Boolean>());
        for(var cause=failure;cause!=null&&seen.add(cause);cause=cause.getCause())causes.add(cause);
        if(causes.stream().anyMatch(SSLException.class::isInstance))return "Connexion sécurisée impossible (TLS). Vérifiez la date et l'heure du poste ; si elles sont correctes, contactez votre administrateur. Vos données locales sont conservées.";
        if(causes.stream().anyMatch(HttpTimeoutException.class::isInstance))return "Le serveur n'a pas répondu dans le délai prévu. Il peut être en cours de démarrage. Patientez puis réessayez. Vos données locales sont conservées.";
        if(causes.stream().anyMatch(c->c instanceof UnknownHostException||c instanceof UnresolvedAddressException))return "Le service est introuvable sur le réseau (DNS). Vérifiez votre connexion Internet puis réessayez.";
        if(causes.stream().anyMatch(ConnectException.class::isInstance))return "Impossible de joindre le service. Vérifiez votre connexion Internet ; si le problème persiste, contactez votre administrateur.";
        if(causes.stream().anyMatch(JsonProcessingException.class::isInstance))return "Le service a renvoyé une réponse inattendue. Réessayez ; si le problème persiste, contactez votre administrateur.";
        return "Cette opération a été interrompue. Votre travail est conservé. Vérifiez votre connexion ou contactez votre administrateur.";
    }
}
