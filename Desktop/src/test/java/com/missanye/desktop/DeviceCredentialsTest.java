package com.missanye.desktop;
import com.missanye.desktop.service.DeviceCredentials;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
class DeviceCredentialsTest{
 @TempDir Path directory;
 // Test-only reversible protector; production uses Windows DPAPI exclusively.
 static DeviceCredentials.Protector testProtector(){return new DeviceCredentials.Protector(){public byte[] protect(byte[] b){return java.util.Base64.getEncoder().encode(b);}public byte[] unprotect(byte[] b){return java.util.Base64.getDecoder().decode(b);}};}
 @Test void passwordChangeKeepsEncryptionKeyAndRevokesOldVerifier()throws Exception{
  var credentials=new DeviceCredentials(directory,testProtector());var first=credentials.prepare("https://api.test/","Alice","old password".toCharArray(),"owner");credentials.save("https://api.test/","Alice",first);
  assertEquals(first.key(),credentials.verify("https://api.test/","alice","old password".toCharArray()).key());assertThrows(IllegalArgumentException.class,()->credentials.verify("https://api.test/","alice","wrong".toCharArray()));
  var changed=credentials.prepare("https://api.test/","alice","new password".toCharArray(),"owner");assertEquals(first.key(),changed.key());credentials.save("https://api.test/","alice",changed);assertThrows(IllegalArgumentException.class,()->credentials.verify("https://api.test/","alice","old password".toCharArray()));assertNotNull(credentials.verify("https://api.test/","alice","new password".toCharArray()));assertNull(credentials.read("https://another.test/","alice"));
 }
 @Test void expiryAndOwnerMismatchFailClosed()throws Exception{
  var credentials=new DeviceCredentials(directory,testProtector());var ticket=credentials.prepare("https://api.test/","alice","password".toCharArray(),"owner");credentials.save("https://api.test/","alice",new DeviceCredentials.Ticket(ticket.owner(),ticket.key(),ticket.salt(),ticket.verifier(),Instant.now().minusSeconds(1).getEpochSecond()));assertThrows(IllegalArgumentException.class,()->credentials.verify("https://api.test/","alice","password".toCharArray()));assertThrows(IllegalArgumentException.class,()->credentials.prepare("https://api.test/","alice","password".toCharArray(),"someone-else"));
 }
 @Test @EnabledOnOs(OS.WINDOWS) void realWindowsProtectionRoundTripsAndRejectsTampering()throws Exception{
  var protector=new DeviceCredentials.WindowsProtector();byte[] plain="device-secret-test-only".getBytes(java.nio.charset.StandardCharsets.UTF_8),sealed=protector.protect(plain);assertFalse(java.util.Arrays.equals(plain,sealed));assertArrayEquals(plain,protector.unprotect(sealed));sealed[sealed.length/2]^=1;assertThrows(Exception.class,()->protector.unprotect(sealed));
 }
}
