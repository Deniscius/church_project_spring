package com.missanye.desktop.service;

import java.nio.file.*;

/** Never truncate the previous PDF if writing the new copy fails. */
public final class ReceiptExporter {
    private ReceiptExporter(){}
    public static void write(Path target,byte[] pdf)throws Exception{
        if(pdf.length<5||!new String(pdf,0,5,java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"))throw new IllegalArgumentException("Le document reçu n'est pas un PDF valide.");
        Path absolute=target.toAbsolutePath();Path temporary=Files.createTempFile(absolute.getParent(),".missanye-receipt-",".tmp");
        try{Files.write(temporary,pdf);try{Files.move(temporary,absolute,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(temporary,absolute,StandardCopyOption.REPLACE_EXISTING);}}
        finally{Files.deleteIfExists(temporary);}
    }
}
