package com.missanye.desktop.service;

import com.fasterxml.jackson.databind.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/** Checks official releases and downloads a size-bounded installer verified against its published SHA-256. */
public final class UpdateService {
    private static final String REPOSITORY="Deniscius/church_project_spring";
    public static String currentVersion(){
        var properties=new Properties();try(var in=UpdateService.class.getResourceAsStream("/com/missanye/desktop/desktop-version.properties")){if(in!=null)properties.load(in);}catch(Exception ignored){}
        return properties.getProperty("version","0.1.0");
    }
    public record Release(String version,URI page,URI download,URI checksum,long size){}
    public static Optional<Release> available(JsonNode releases,String current){
        Release newest=null;for(var release:releases){
            if(release.path("draft").asBoolean()||release.path("prerelease").asBoolean())continue;
            String tag=release.path("tag_name").asText();if(!tag.matches("desktop-v\\d+\\.\\d+\\.\\d+"))continue;String version=tag.substring(9);
            URI page=safe(release.path("html_url").asText(),"/"+REPOSITORY+"/releases/tag/");if(page==null||!page.getPath().equals("/"+REPOSITORY+"/releases/tag/"+tag))continue;
            URI checksum=null;
            for(var asset:release.path("assets"))if("Missanye-Setup.exe.sha256".equals(asset.path("name").asText())){
                URI candidate=safe(asset.path("browser_download_url").asText(),"/"+REPOSITORY+"/releases/download/");
                if(candidate!=null&&candidate.getPath().equals("/"+REPOSITORY+"/releases/download/"+tag+"/Missanye-Setup.exe.sha256"))checksum=candidate;
            }
            for(var asset:release.path("assets"))if("Missanye-Setup.exe".equals(asset.path("name").asText())&&asset.path("size").asLong()>0){
                URI download=safe(asset.path("browser_download_url").asText(),"/"+REPOSITORY+"/releases/download/");
                if(download!=null&&download.getPath().equals("/"+REPOSITORY+"/releases/download/"+tag+"/Missanye-Setup.exe")&&checksum!=null&&asset.path("size").asLong()<=MAX_INSTALLER_BYTES&&compare(version,current)>0&&(newest==null||compare(version,newest.version())>0))newest=new Release(version,page,download,checksum,asset.path("size").asLong());
            }
        }return Optional.ofNullable(newest);
    }
    private static URI safe(String url,String prefix){try{var uri=URI.create(url);return "https".equals(uri.getScheme())&&"github.com".equals(uri.getHost())&&uri.getUserInfo()==null&&uri.getPort()==-1&&uri.getQuery()==null&&uri.getFragment()==null&&uri.normalize().getPath().startsWith(prefix)?uri:null;}catch(Exception invalid){return null;}}
    public static int compare(String left,String right){try{String[] a=left.replaceFirst("[-+].*$","").split("\\."),b=right.replaceFirst("[-+].*$","").split("\\.");for(int i=0;i<3;i++){int c=Integer.compare(Integer.parseInt(a[i]),Integer.parseInt(b[i]));if(c!=0)return c;}return 0;}catch(Exception invalid){return 0;}}
    private static final long MAX_INSTALLER_BYTES=250_000_000;
    private static HttpClient downloadClient(){return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NORMAL).build();}
    private static java.io.InputStream download(HttpClient client,URI uri)throws Exception{
        var response=client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(3)).header("User-Agent","Missanye-Desktop/"+currentVersion()).GET().build(),HttpResponse.BodyHandlers.ofInputStream());
        String host=response.uri().getHost();
        if(response.statusCode()!=200||!"https".equals(response.uri().getScheme())||!Set.of("github.com","release-assets.githubusercontent.com","objects.githubusercontent.com").contains(host)){
            response.body().close();throw new java.io.IOException("Téléchargement de la mise à jour indisponible.");
        }
        return response.body();
    }
    public static void verifyInstaller(java.nio.file.Path file,String checksum,long expectedSize)throws Exception{
        String hash=checksum.strip();
        if(!hash.matches("(?i)[0-9a-f]{64}")||expectedSize<=0||expectedSize>MAX_INSTALLER_BYTES||java.nio.file.Files.size(file)!=expectedSize)throw new java.io.IOException("Taille ou empreinte de la mise à jour invalide.");
        var digest=java.security.MessageDigest.getInstance("SHA-256");
        try(var in=java.nio.file.Files.newInputStream(file)){byte[] buffer=new byte[65536];int count;while((count=in.read(buffer))!=-1)digest.update(buffer,0,count);}
        if(!java.security.MessageDigest.isEqual(HexFormat.of().parseHex(hash),digest.digest()))throw new java.io.IOException("La vérification de la mise à jour a échoué. Réessayez le téléchargement.");
        try(var in=java.nio.file.Files.newInputStream(file)){if(in.read()!='M'||in.read()!='Z')throw new java.io.IOException("Le fichier reçu n’est pas un installateur Windows.");}
    }
    public java.nio.file.Path prepare(Release release,java.nio.file.Path directory)throws Exception{
        // Revalidate even when a caller supplies a Release directly.
        String prefix="/"+REPOSITORY+"/releases/download/desktop-v"+release.version()+"/";
        if(!release.version().matches("\\d+\\.\\d+\\.\\d+")||safe(release.download().toString(),prefix)==null||!release.download().getPath().equals(prefix+"Missanye-Setup.exe")||release.checksum()==null||safe(release.checksum().toString(),prefix)==null||!release.checksum().getPath().equals(prefix+"Missanye-Setup.exe.sha256")||release.size()<=0||release.size()>MAX_INSTALLER_BYTES)throw new java.io.IOException("Version de mise à jour invalide.");
        var client=downloadClient();String checksum;
        try(var in=download(client,release.checksum())){byte[] bytes=in.readNBytes(1025);if(bytes.length>1024)throw new java.io.IOException("Empreinte de mise à jour invalide.");checksum=new String(bytes,java.nio.charset.StandardCharsets.UTF_8);}
        java.nio.file.Files.createDirectories(directory);var temporary=java.nio.file.Files.createTempFile(directory,"update-",".tmp");
        try{
            try(var in=download(client,release.download());var out=java.nio.file.Files.newOutputStream(temporary)){
                byte[] buffer=new byte[65536];long total=0;int count;while((count=in.read(buffer))!=-1){total+=count;if(total>release.size()||Thread.currentThread().isInterrupted())throw new java.io.IOException("Téléchargement interrompu ou trop volumineux.");out.write(buffer,0,count);}
            }
            verifyInstaller(temporary,checksum,release.size());var target=directory.resolve("Missanye-Setup-"+release.version()+".exe");java.nio.file.Files.move(temporary,target,java.nio.file.StandardCopyOption.REPLACE_EXISTING);return target;
        }finally{java.nio.file.Files.deleteIfExists(temporary);}
    }
    public static void install(java.nio.file.Path installer)throws Exception{
        if(!System.getProperty("os.name","").startsWith("Windows"))throw new java.io.IOException("La mise à jour intégrée nécessite Windows.");
        String command="$ErrorActionPreference = 'Stop'; Start-Process -FilePath '"+installer.toAbsolutePath().toString().replace("'","''")+"' -Verb RunAs -ArgumentList '/SILENT','/SUPPRESSMSGBOXES','/NORESTART','/SP-','/MISSANYEUPDATE=1' | Out-Null";
        String encoded=Base64.getEncoder().encodeToString(command.getBytes(java.nio.charset.StandardCharsets.UTF_16LE));
        var process=new ProcessBuilder("powershell.exe","-NoProfile","-NonInteractive","-EncodedCommand",encoded).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        if(!process.waitFor(120,java.util.concurrent.TimeUnit.SECONDS)){process.destroy();throw new java.io.IOException("L’autorisation administrateur a expiré.");}
        if(process.exitValue()!=0)throw new java.io.IOException("L’installation n’a pas été autorisée par Windows.");
    }
    public Optional<Release> check()throws Exception{
        var request=HttpRequest.newBuilder(URI.create("https://api.github.com/repos/"+REPOSITORY+"/releases?per_page=30")).timeout(Duration.ofSeconds(15)).header("Accept","application/vnd.github+json").header("User-Agent","Missanye-Desktop/"+currentVersion()).GET().build();
        var response=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build().send(request,HttpResponse.BodyHandlers.ofString());
        if(response.statusCode()!=200)throw new java.io.IOException("Le catalogue des versions est momentanément indisponible.");var body=new ObjectMapper().readTree(response.body());if(!body.isArray())throw new java.io.IOException("Catalogue des versions invalide.");return available(body,currentVersion());
    }
}
