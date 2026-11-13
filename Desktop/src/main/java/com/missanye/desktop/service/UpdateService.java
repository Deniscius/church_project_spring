package com.missanye.desktop.service;

import com.fasterxml.jackson.databind.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/** Reads published desktop installers only. Never downloads or executes software automatically. */
public final class UpdateService {
    private static final String REPOSITORY="Deniscius/church_project_spring";
    public static String currentVersion(){
        var properties=new Properties();try(var in=UpdateService.class.getResourceAsStream("/com/missanye/desktop/desktop-version.properties")){if(in!=null)properties.load(in);}catch(Exception ignored){}
        return properties.getProperty("version","0.1.0");
    }
    public record Release(String version,URI page,URI download){}
    public static Optional<Release> available(JsonNode releases,String current){
        Release newest=null;for(var release:releases){
            if(release.path("draft").asBoolean()||release.path("prerelease").asBoolean())continue;
            String tag=release.path("tag_name").asText();if(!tag.matches("desktop-v\\d+\\.\\d+\\.\\d+"))continue;String version=tag.substring(9);
            URI page=safe(release.path("html_url").asText(),"/"+REPOSITORY+"/releases/tag/");if(page==null||!page.getPath().equals("/"+REPOSITORY+"/releases/tag/"+tag))continue;
            for(var asset:release.path("assets"))if("Missanye-Setup.exe".equals(asset.path("name").asText())&&asset.path("size").asLong()>0){
                URI download=safe(asset.path("browser_download_url").asText(),"/"+REPOSITORY+"/releases/download/");
                if(download!=null&&download.getPath().equals("/"+REPOSITORY+"/releases/download/"+tag+"/Missanye-Setup.exe")&&compare(version,current)>0&&(newest==null||compare(version,newest.version())>0))newest=new Release(version,page,download);
            }
        }return Optional.ofNullable(newest);
    }
    private static URI safe(String url,String prefix){try{var uri=URI.create(url);return "https".equals(uri.getScheme())&&"github.com".equals(uri.getHost())&&uri.getUserInfo()==null&&uri.getPort()==-1&&uri.getQuery()==null&&uri.getFragment()==null&&uri.normalize().getPath().startsWith(prefix)?uri:null;}catch(Exception invalid){return null;}}
    public static int compare(String left,String right){try{String[] a=left.replaceFirst("[-+].*$","").split("\\."),b=right.replaceFirst("[-+].*$","").split("\\.");for(int i=0;i<3;i++){int c=Integer.compare(Integer.parseInt(a[i]),Integer.parseInt(b[i]));if(c!=0)return c;}return 0;}catch(Exception invalid){return 0;}}
    public Optional<Release> check()throws Exception{
        var request=HttpRequest.newBuilder(URI.create("https://api.github.com/repos/"+REPOSITORY+"/releases?per_page=30")).timeout(Duration.ofSeconds(15)).header("Accept","application/vnd.github+json").header("User-Agent","Missanye-Desktop/"+currentVersion()).GET().build();
        var response=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build().send(request,HttpResponse.BodyHandlers.ofString());
        if(response.statusCode()!=200)throw new java.io.IOException("Le catalogue des versions est momentanément indisponible.");var body=new ObjectMapper().readTree(response.body());if(!body.isArray())throw new java.io.IOException("Catalogue des versions invalide.");return available(body,currentVersion());
    }
}
