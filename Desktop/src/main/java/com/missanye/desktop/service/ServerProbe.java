package com.missanye.desktop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.missanye.desktop.config.DesktopConfiguration;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;

/** No credentials or session cookies are sent during endpoint discovery. */
public final class ServerProbe {
    private ServerProbe(){}
    public static void test(String address)throws Exception{
        String base=new DesktopConfiguration(address).apiBaseUrl();
        var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NEVER).build();
        var request=HttpRequest.newBuilder(URI.create(base).resolve("actuator/health")).timeout(Duration.ofMinutes(2)).header("Accept","application/json").GET().build();
        var response=client.send(request,HttpResponse.BodyHandlers.ofString());
        if(response.statusCode()!=200)throw new IllegalArgumentException("Le service ne répond pas correctement au contrôle de santé (HTTP "+response.statusCode()+"). Vérifiez l'adresse du backend.");
        var result=new ObjectMapper().readTree(response.body());
        if(result==null||!"UP".equals(result.path("status").asText()))throw new IllegalArgumentException("Le serveur est accessible, mais son état de santé ne permet pas la connexion. Vérifiez le backend et sa base de données.");
    }
}
