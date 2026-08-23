package com.eyram.dev.church_project_spring.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseUrlBootstrapTest {

    @Test
    void parsesRenderPostgresUrl() {
        var parsed = DatabaseUrlBootstrap.parse(
                "postgres://missanye:s3cret%21@dpg-abc-a.oregon-postgres.render.com:5432/missanye");
        assertNotNull(parsed);
        assertEquals("missanye", parsed.username());
        assertEquals("s3cret!", parsed.password());
        assertTrue(parsed.jdbcUrl().startsWith("jdbc:postgresql://dpg-abc-a.oregon-postgres.render.com:5432/missanye"));
        assertTrue(parsed.jdbcUrl().contains("sslmode=require"));
    }

    @Test
    void preservesLiteralPlusInCredentials() {
        var parsed = DatabaseUrlBootstrap.parse(
                "postgres://user%2Bprod:p+a%2Bss@localhost:5432/db");
        assertNotNull(parsed);
        assertEquals("user+prod", parsed.username());
        assertEquals("p+a+ss", parsed.password());
    }

    @Test
    void preservesExistingSslMode() {
        var parsed = DatabaseUrlBootstrap.parse(
                "postgresql://u:p@localhost:5432/db?sslmode=disable");
        assertNotNull(parsed);
        assertTrue(parsed.jdbcUrl().contains("sslmode=disable"));
        assertTrue(!parsed.jdbcUrl().contains("sslmode=require"));
    }

    @Test
    void rejectsUnknownScheme() {
        assertNull(DatabaseUrlBootstrap.parse("mysql://u:p@localhost/db"));
    }
}
