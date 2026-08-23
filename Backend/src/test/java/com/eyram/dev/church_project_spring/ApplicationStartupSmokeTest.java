package com.eyram.dev.church_project_spring;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

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

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void applicationStartsAndExposesHealthyReadinessEndpoint() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/actuator/health/readiness", String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }
}
