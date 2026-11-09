package com.missanye.desktop.config;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/** Per-installation endpoint; account caches remain scoped to the full server URL. */
public final class ServerSettings {
    private ServerSettings(){}
    public static Path file(Path database){return database.toAbsolutePath().resolveSibling("server.properties");}
    public static String load(Path database,String fallback)throws IOException{
        Path file=file(database);if(!Files.exists(file))return new DesktopConfiguration(fallback).apiBaseUrl();
        var values=new Properties();try(var in=Files.newInputStream(file)){values.load(in);}
        return new DesktopConfiguration(values.getProperty("api.base-url")).apiBaseUrl();
    }
    public static void save(Path database,String address)throws IOException{
        String validated=new DesktopConfiguration(address).apiBaseUrl();Path file=file(database);Files.createDirectories(file.getParent());
        Path temporary=Files.createTempFile(file.getParent(),"server-",".tmp");
        try{
            var values=new Properties();values.setProperty("api.base-url",validated);
            try(var out=Files.newOutputStream(temporary)){values.store(out,"Missanye server configuration - no credentials");}
            try{Files.move(temporary,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(temporary);}
    }
}
