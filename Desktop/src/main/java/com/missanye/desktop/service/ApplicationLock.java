package com.missanye.desktop.service;

import java.nio.channels.*;
import java.nio.file.*;

/** One running application per data directory, including after a crash/restart. */
public final class ApplicationLock implements AutoCloseable {
    public static final class AlreadyRunningException extends IllegalStateException {public AlreadyRunningException(){super("Missanye est déjà ouvert sur ce poste. Retrouvez sa fenêtre dans la barre des tâches.");}}
    private final FileChannel channel;
    private final FileLock lock;
    private ApplicationLock(FileChannel channel,FileLock lock){this.channel=channel;this.lock=lock;}
    public static ApplicationLock acquire(Path directory)throws Exception{
        Files.createDirectories(directory);var channel=FileChannel.open(directory.resolve("desktop.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
        try{var lock=channel.tryLock();if(lock==null)throw new AlreadyRunningException();return new ApplicationLock(channel,lock);}
        catch(Exception e){channel.close();if(e instanceof OverlappingFileLockException)throw new AlreadyRunningException();throw e;}
    }
    @Override public void close()throws Exception{try{lock.release();}finally{channel.close();}}
}
