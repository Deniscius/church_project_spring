package com.missanye.desktop;

import com.missanye.desktop.api.ApiClient;
import com.missanye.desktop.service.DataRevision;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DataRevisionTest {
    @Test void conditionalChecksDetectRemoteChangesAndPeriodicallyReconcile()throws Exception{
        var now=new AtomicLong(1000);var version=new AtomicReference<>("a".repeat(64));var requests=new AtomicInteger();
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/desktop/",e->{requests.incrementAndGet();if(("\""+version.get()+"\"").equals(e.getRequestHeaders().getFirst("If-None-Match"))){e.sendResponseHeaders(304,-1);}else{var bytes=("{\"revision\":\""+version.get()+"\"}").getBytes(StandardCharsets.UTF_8);e.sendResponseHeaders(200,bytes.length);e.getResponseBody().write(bytes);}e.close();});server.start();
        try{var api=new ApiClient("http://127.0.0.1:"+server.getAddress().getPort()+"/");var tracker=new DataRevision(now::get);var first=tracker.check(api,TestRequests.PARISH);assertTrue(first.changed());tracker.acknowledge(first);now.addAndGet(10000);assertFalse(tracker.check(api,TestRequests.PARISH).changed());version.set("b".repeat(64));var changed=tracker.check(api,TestRequests.PARISH);assertTrue(changed.changed());assertTrue(tracker.check(api,TestRequests.PARISH).changed(),"A failed reload must not acknowledge the new revision");tracker.acknowledge(changed);assertFalse(tracker.check(api,TestRequests.PARISH).changed());now.addAndGet(60000);assertTrue(tracker.check(api,TestRequests.PARISH).changed());assertEquals(6,requests.get());}finally{server.stop(0);}
    }
    @Test void olderServersUseTimedReconciliationWithoutRepeatedMissingEndpointRequests()throws Exception{
        var now=new AtomicLong(1000);var requests=new AtomicInteger();var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/desktop/",e->{requests.incrementAndGet();e.sendResponseHeaders(404,-1);e.close();});server.start();
        try{var api=new ApiClient("http://127.0.0.1:"+server.getAddress().getPort()+"/");var tracker=new DataRevision(now::get);var first=tracker.check(api,TestRequests.PARISH);assertTrue(first.changed());tracker.acknowledge(first);now.addAndGet(10000);assertFalse(tracker.check(api,TestRequests.PARISH).changed());now.addAndGet(60000);assertTrue(tracker.check(api,TestRequests.PARISH).changed());assertEquals(1,requests.get());}finally{server.stop(0);}
    }
}
