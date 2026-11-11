package com.missanye.desktop;

import com.missanye.desktop.api.ApiClient;
import com.missanye.desktop.service.WorkspaceBootstrap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

/** Disposable empty DB, seeded by AdminUserSeeder with these test-only credentials; HTTPS required. */
@EnabledIfEnvironmentVariable(named="DESKTOP_SEED_E2E",matches="true")
class LiveSeedAccountTest {
    @Test void initialSeedAdminAuthenticatesWithSecureCookieBeforeAnyTenantExists()throws Exception{
        var api=new ApiClient(System.getenv().getOrDefault("DESKTOP_SEED_API_URL","https://localhost:18444"));
        var session=api.login("desktop-seed-admin","CiDesktopSeed-2026!");
        assertFalse(session.path("user").path("id").asText().isBlank());assertEquals("SUPER_ADMIN",session.path("user").path("role").asText());assertTrue(session.path("user").path("isGlobal").asBoolean());
        assertEquals(session.path("user").path("id"),api.get("auth/me").path("user").path("id"));
        assertTrue(WorkspaceBootstrap.eligible(api.get("paroisses")).isEmpty());api.logout();
        var refused=assertThrows(ApiClient.ApiException.class,()->api.login("desktop-seed-admin","Incorrect test password"));assertEquals(401,refused.status());assertTrue(refused.getMessage().contains("Identifiant ou mot de passe refusé"));
    }
}
