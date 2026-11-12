package com.missanye.desktop;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.missanye.desktop.service.UpdateService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UpdateServiceTest {
    private final ObjectMapper json=new ObjectMapper();
    private ObjectNode release(String version){String tag="desktop-v"+version;var item=json.createObjectNode().put("tag_name",tag).put("html_url","https://github.com/Deniscius/church_project_spring/releases/tag/"+tag);item.putArray("assets").addObject().put("name","Missanye-Setup.exe").put("size",100).put("browser_download_url","https://github.com/Deniscius/church_project_spring/releases/download/"+tag+"/Missanye-Setup.exe");return item;}
    @Test void onlyNewPublishedDesktopInstallersAreProposed(){
        var releases=json.createArrayNode().add(release("0.2.0")).add(release("0.10.0")).add(release("9.0.0").put("draft",true)).add(release("10.0.0").put("prerelease",true));assertEquals("0.10.0",UpdateService.available(releases,"0.1.0").orElseThrow().version());assertTrue(UpdateService.available(releases,"0.10.0").isEmpty());
    }
    @Test void untrustedLinksAndReleasesWithoutAnInstallerAreIgnored(){
        var unsafe=release("1.0.0");((ObjectNode)unsafe.path("assets").get(0)).put("browser_download_url","https://evil.example/Missanye-Setup.exe");assertTrue(UpdateService.available(json.createArrayNode().add(unsafe),"0.1.0").isEmpty());
        var noInstaller=release("1.0.0");noInstaller.remove("assets");assertTrue(UpdateService.available(json.createArrayNode().add(noInstaller),"0.1.0").isEmpty());
        var otherRepo=release("1.0.0").put("html_url","https://github.com/another/repo/releases/tag/desktop-v1.0.0");assertTrue(UpdateService.available(json.createArrayNode().add(otherRepo),"0.1.0").isEmpty());assertTrue(UpdateService.currentVersion().matches("\\d+\\.\\d+\\.\\d+"));
    }
}
