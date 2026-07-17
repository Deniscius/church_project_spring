package com.eyram.dev.church_project_spring.support;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base commune pour les tests d'intégration Spring.
 *
 * Les propriétés déclarées ici ont une priorité supérieure aux variables
 * d'environnement locales. Ainsi, les tests ne peuvent jamais utiliser par
 * erreur la base PostgreSQL de développement, même si des variables
 * SPRING_DATASOURCE_* sont encore définies dans le terminal.
 */
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    private static final String TEST_DATABASE_URL =
            "jdbc:h2:mem:church_project_test;MODE=PostgreSQL;" +
            "DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";

    private static final String TEST_JWT_SECRET =
            "dGVzdFNlY3JldEtleUZvckp3dDI1NkJpdHNNb25seQ==";

    @DynamicPropertySource
    static void registerTestProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> TEST_DATABASE_URL);
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.open-in-view", () -> "false");

        registry.add("jwt.secret", () -> TEST_JWT_SECRET);
        registry.add("app.admin-seed.enabled", () -> "false");
    }
}
