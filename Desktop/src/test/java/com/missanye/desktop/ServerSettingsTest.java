package com.missanye.desktop;

import com.missanye.desktop.config.ServerSettings;
import com.missanye.desktop.service.ServerProbe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;
import static org.junit.jupiter.api.Assertions.*;

class ServerSettingsTest {
    @TempDir Path temp;
    @Test void persistsValidatedAddressAndPreservesPreviousConfigurationOnInvalidSave()throws Exception{
        Path db=temp.resolve("workflow.sqlite");String original="https://example.org/api/";
        assertEquals(original,ServerSettings.load(db,original));assertFalse(Files.exists(ServerSettings.file(db)));
        ServerSettings.save(db," https://example.com/api ");assertEquals("https://example.com/api/",ServerSettings.load(db,original));
        assertThrows(IllegalArgumentException.class,()->ServerSettings.save(db,"https://user:password@example.com"));
        assertThrows(IllegalArgumentException.class,()->ServerSettings.save(db,"http://example.com"));
        assertEquals("https://example.com/api/",ServerSettings.load(db,original));
        assertFalse(Files.exists(db));
    }
    @Test void probeUsesHealthWithoutCredentialsAndRejectsUnavailableOrUnrelatedServer()throws Exception{
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);var answer=new java.util.concurrent.atomic.AtomicReference<>("{\"status\":\"UP\"}");
        var credential=new java.util.concurrent.atomic.AtomicBoolean();
        server.createContext("/api/actuator/health",e->{credential.set(e.getRequestHeaders().containsKey("Cookie")||e.getRequestHeaders().containsKey("Authorization"));byte[] body=answer.get().getBytes(java.nio.charset.StandardCharsets.UTF_8);e.sendResponseHeaders(200,body.length);e.getResponseBody().write(body);e.close();});server.start();
        String address="http://127.0.0.1:"+server.getAddress().getPort()+"/api/";
        try{ServerProbe.test(address);assertFalse(credential.get());answer.set("{\"status\":\"DOWN\"}");assertThrows(Exception.class,()->ServerProbe.test(address));answer.set("<html>Frontend</html>");assertThrows(Exception.class,()->ServerProbe.test(address));assertThrows(IllegalArgumentException.class,()->ServerProbe.test(address+"missing/"));}
        finally{server.stop(0);}
    }
    @Test void parishProfileCanBeBundledAndOverriddenByValidatedLocalDeployment()throws Exception{
        Path bundledDb=temp.resolve("bundle/ignored.sqlite"),localDb=temp.resolve("local/workflow.sqlite");
        var bundled=new com.missanye.desktop.config.ParishDeployment("https://api.example.org",TestRequests.PARISH,"Saint Paul");ServerSettings.saveDeployment(bundledDb,bundled);
        String previous=System.getProperty("missanye.deploymentFile");System.setProperty("missanye.deploymentFile",ServerSettings.file(bundledDb).toString());
        try{assertEquals(bundled,ServerSettings.deployment(localDb).orElseThrow());var local=new com.missanye.desktop.config.ParishDeployment("https://api.example.org","731c7ab4-2fae-4d78-a17b-0ff7653f4599","Saint Pierre");ServerSettings.saveDeployment(localDb,local);assertEquals(local,ServerSettings.deployment(localDb).orElseThrow());String text=java.nio.file.Files.readString(ServerSettings.file(localDb));assertFalse(text.contains("password"));assertFalse(text.contains("token"));}
        finally{if(previous==null)System.clearProperty("missanye.deploymentFile");else System.setProperty("missanye.deploymentFile",previous);}
    }

}
