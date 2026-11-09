package com.missanye.desktop.config;

import java.io.IOException;
import java.net.URI;
import java.util.Properties;

/** Deployment-owned settings. There is deliberately no end-user editor. */
public record DesktopConfiguration(String apiBaseUrl) {
    public DesktopConfiguration {
        if(apiBaseUrl==null || apiBaseUrl.isBlank())throw new IllegalArgumentException("Adresse API absente de la configuration de livraison.");
        URI uri=URI.create(apiBaseUrl.trim());
        boolean local="localhost".equals(uri.getHost()) || "127.0.0.1".equals(uri.getHost()) || "[::1]".equals(uri.getHost());
        if(uri.getHost()==null || uri.getUserInfo()!=null || uri.getQuery()!=null || uri.getFragment()!=null
                || !("https".equals(uri.getScheme()) || (local && "http".equals(uri.getScheme()))))
            throw new IllegalArgumentException("Configuration de livraison invalide : HTTPS requis hors développement local.");
        apiBaseUrl=uri.toString();if(!apiBaseUrl.endsWith("/"))apiBaseUrl+="/";
    }
    public static DesktopConfiguration load() {
        var values=new Properties();
        try(var stream=DesktopConfiguration.class.getResourceAsStream("deployment.properties")) {
            if(stream==null)throw new IllegalStateException("Configuration de livraison absente.");
            values.load(stream);
        }catch(IOException e){throw new IllegalStateException("Configuration de livraison illisible.",e);}
        return resolve(System.getProperty("missanye.apiUrl"),System.getenv("MISSANYE_API_URL"),values);
    }
    public static DesktopConfiguration resolve(String systemOverride,String environmentOverride,Properties bundled) {
        String value=systemOverride;
        if(value==null || value.isBlank())value=environmentOverride;
        if(value==null || value.isBlank())value=bundled.getProperty("api.base-url");
        return new DesktopConfiguration(value);
    }
}
