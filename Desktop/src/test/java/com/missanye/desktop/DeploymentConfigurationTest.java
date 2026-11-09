package com.missanye.desktop;

import com.missanye.desktop.config.DesktopConfiguration;
import org.junit.jupiter.api.Test;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

class DeploymentConfigurationTest {
    @Test void deploymentAddressIsBundledWithoutUserInput(){var config=DesktopConfiguration.load();assertNotNull(config.apiBaseUrl());assertNotNull(java.net.URI.create(config.apiBaseUrl()).getHost());assertFalse(config.apiBaseUrl().contains("${"));}
    @Test void developerOverridesHaveExplicitPrecedence(){var p=new Properties();p.setProperty("api.base-url","https://bundled.example");assertEquals("https://system.example/",DesktopConfiguration.resolve("https://system.example","https://env.example",p).apiBaseUrl());assertEquals("https://env.example/",DesktopConfiguration.resolve("","https://env.example",p).apiBaseUrl());assertEquals("https://bundled.example/",DesktopConfiguration.resolve(null,null,p).apiBaseUrl());}
    @Test void onlyLocalDevelopmentMayUsePlainHttp(){assertEquals("http://localhost:8081/",new DesktopConfiguration("http://localhost:8081").apiBaseUrl());assertThrows(IllegalArgumentException.class,()->new DesktopConfiguration("http://api.example.com"));assertThrows(IllegalArgumentException.class,()->new DesktopConfiguration("https://user:secret@api.example.com"));assertThrows(IllegalArgumentException.class,()->new DesktopConfiguration("https://api.example.com?token=secret"));}
    @Test void missingConfigurationIsRejected(){assertThrows(IllegalArgumentException.class,()->DesktopConfiguration.resolve(null,null,new Properties()));}
}
