package com.missanye.desktop.config;
import java.util.UUID;
public record ParishDeployment(String apiUrl,String parishId,String parishName){
    public ParishDeployment{apiUrl=new DesktopConfiguration(apiUrl).apiBaseUrl();UUID.fromString(parishId);if(parishName==null||parishName.isBlank())throw new IllegalArgumentException("Nom de paroisse requis");}
}
