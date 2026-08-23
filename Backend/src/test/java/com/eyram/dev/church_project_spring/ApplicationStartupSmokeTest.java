package com.eyram.dev.church_project_spring;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.eyram.dev.church_project_spring.entities.User;
import com.eyram.dev.church_project_spring.enums.UserRole;
import com.eyram.dev.church_project_spring.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.admin-seed.enabled=false",
                "app.tenant-catalog.backfill-on-startup=false",
                "app.demande.unpaid-auto-cancel-enabled=false",
                "app.demande.unpaid-reminder-enabled=false",
                "app.demande.celebration-reminder-enabled=false",
                "app.demande.celebration-auto-complete-enabled=false",
                "fedapay.enabled=false",
                "spring.flyway.repair-on-migrate=false"
        }
)
@EnabledIfEnvironmentVariable(named = "CI_POSTGRES_SMOKE", matches = "true")
class ApplicationStartupSmokeTest {

    private static final String USERNAME = "ci-platform-admin";
    private static final String PASSWORD = "CiStrongPassword-2026!";

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void ensurePlatformUserExists() {
        if (userRepository.existsByUsernameIgnoreCaseAndStatusDelFalse(USERNAME)) {
            return;
        }

        User user = new User();
        user.setNom("Administrateur");
        user.setPrenom("CI");
        user.setUsername(USERNAME);
        user.setEmail("ci-admin@example.test");
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setRole(UserRole.SUPER_ADMIN);
        user.setIsGlobal(true);
        user.setIsActive(true);
        user.setStatusDel(false);
        userRepository.save(user);
    }

    @Test
    void applicationStartsAndExposesHealthyReadinessEndpoint() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/actuator/health/readiness", String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void protectedEndpointRejectsAnonymousRequest() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/admin/users", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getHeaders().getFirst("X-Content-Type-Options"))
                .isEqualTo("nosniff");
    }

    @Test
    void loginUsesHttpOnlyCookieAndAuthenticatesMeEndpoint() {
        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> loginRequest = new HttpEntity<>(
                Map.of("username", USERNAME, "password", PASSWORD),
                loginHeaders
        );

        ResponseEntity<String> loginResponse =
                restTemplate.postForEntity("/auth/login", loginRequest, String.class);

        assertThat(loginResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(loginResponse.getBody()).doesNotContain("eyJ");

        String setCookie = loginResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie)
                .isNotBlank()
                .contains("MS_AT=")
                .contains("HttpOnly")
                .contains("SameSite=Lax");

        HttpHeaders authenticatedHeaders = new HttpHeaders();
        authenticatedHeaders.set(HttpHeaders.COOKIE, setCookie.split(";", 2)[0]);

        ResponseEntity<String> meResponse = restTemplate.exchange(
                "/auth/me",
                HttpMethod.GET,
                new HttpEntity<>(authenticatedHeaders),
                String.class
        );

        assertThat(meResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(meResponse.getBody())
                .contains(USERNAME)
                .contains("\"isGlobal\":true");
    }
}
