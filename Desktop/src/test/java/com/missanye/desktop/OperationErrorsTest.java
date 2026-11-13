package com.missanye.desktop;

import com.missanye.desktop.service.OperationErrors;
import com.missanye.desktop.api.ApiClient;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.*;
import java.net.http.HttpTimeoutException;
import javax.net.ssl.SSLHandshakeException;
import static org.junit.jupiter.api.Assertions.*;

class OperationErrorsTest {
    @Test void distinguishesTimeoutTlsAndConnectivityWithoutLeakingDetails(){
        assertTrue(OperationErrors.message(new HttpTimeoutException("private request")).contains("délai"));
        assertTrue(OperationErrors.message(new IOException(new SSLHandshakeException("private request"))).contains("TLS"));
        assertTrue(OperationErrors.message(new IOException(new UnknownHostException("private host"))).contains("DNS"));
        assertTrue(OperationErrors.message(new ConnectException("private host")).contains("joindre"));
        assertFalse(OperationErrors.message(new SSLHandshakeException("secret")).contains("secret"));
        assertFalse(OperationErrors.message(new IOException("secret")).contains("secret"));
    }
    @Test void serverFailureIncludesStatusWithoutExposingBackendDetails(){
        var failure=new ApiClient.ApiException(500,"password=private backend stacktrace");String message=OperationErrors.message(failure);
        assertTrue(message.contains("HTTP 500"));assertFalse(message.contains("private"));assertFalse(message.contains("password"));
    }
    @Test void authenticationFailureKeepsItsSpecificMessage(){
        var refused=new ApiClient.ApiException(401,"private backend details");assertEquals(refused.getMessage(),OperationErrors.message(refused));
        assertFalse(OperationErrors.message(refused).contains("private"));
    }
}
