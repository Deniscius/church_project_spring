package com.eyram.dev.church_project_spring.config;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Convertit {@code DATABASE_URL} (format Render / Heroku :
 * {@code postgres://user:pass@host:port/db}) en propriétés JDBC Spring
 * si {@code SPRING_DATASOURCE_URL} n'est pas déjà défini.
 */
public final class DatabaseUrlBootstrap {

    private DatabaseUrlBootstrap() {
    }

    public static void applyIfPresent() {
        String existingJdbc = firstJdbcUrl(
                System.getProperty("spring.datasource.url"),
                envOrProp("SPRING_DATASOURCE_URL"));
        if (existingJdbc != null) {
            return;
        }

        String candidate = firstNonBlank(
                envOrProp("DATABASE_URL"),
                envOrProp("SPRING_DATASOURCE_URL"),
                System.getProperty("SPRING_DATASOURCE_URL"));
        if (!hasText(candidate)) {
            return;
        }
        ParsedDatasource parsed = parse(candidate.trim());
        if (parsed == null) {
            System.err.println("DATABASE_URL: format non reconnu (attendu postgres://…)");
            return;
        }
        System.setProperty("spring.datasource.url", parsed.jdbcUrl());
        System.setProperty("SPRING_DATASOURCE_URL", parsed.jdbcUrl());
        if (hasText(parsed.username())) {
            System.setProperty("spring.datasource.username", parsed.username());
            System.setProperty("SPRING_DATASOURCE_USERNAME", parsed.username());
        }
        if (parsed.password() != null) {
            System.setProperty("spring.datasource.password", parsed.password());
            System.setProperty("SPRING_DATASOURCE_PASSWORD", parsed.password());
        }
    }

    private static String firstJdbcUrl(String... values) {
        for (String value : values) {
            if (hasText(value) && value.toLowerCase(Locale.ROOT).startsWith("jdbc:")) {
                return value;
            }
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    /**
     * @return null si l'URL n'est pas un schéma postgres/postgresql
     */
    static ParsedDatasource parse(String databaseUrl) {
        String raw = databaseUrl.trim();
        String lower = raw.toLowerCase(Locale.ROOT);
        String remainder;
        if (lower.startsWith("postgres://")) {
            remainder = raw.substring("postgres://".length());
        } else if (lower.startsWith("postgresql://")) {
            remainder = raw.substring("postgresql://".length());
        } else if (lower.startsWith("jdbc:postgresql://")) {
            return parseJdbc(raw);
        } else {
            return null;
        }

        int at = remainder.lastIndexOf('@');
        if (at <= 0) {
            return null;
        }
        String userInfo = remainder.substring(0, at);
        String hostAndPath = remainder.substring(at + 1);
        int colon = userInfo.indexOf(':');
        if (colon < 0) {
            return null;
        }
        String username = decode(userInfo.substring(0, colon));
        String password = decode(userInfo.substring(colon + 1));

        String hostPortDb = hostAndPath;
        String query = "";
        int q = hostAndPath.indexOf('?');
        if (q >= 0) {
            query = hostAndPath.substring(q + 1);
            hostPortDb = hostAndPath.substring(0, q);
        }

        int slash = hostPortDb.indexOf('/');
        if (slash < 0) {
            return null;
        }
        String hostPort = hostPortDb.substring(0, slash);
        String database = hostPortDb.substring(slash + 1);
        if (database.isBlank() || hostPort.isBlank()) {
            return null;
        }

        String jdbcQuery = ensureSslMode(query);
        String jdbcUrl = "jdbc:postgresql://" + hostPort + "/" + database
                + (jdbcQuery.isEmpty() ? "" : "?" + jdbcQuery);
        return new ParsedDatasource(jdbcUrl, username, password);
    }

    private static ParsedDatasource parseJdbc(String jdbcUrl) {
        String withSsl = jdbcUrl.contains("sslmode=")
                ? jdbcUrl
                : jdbcUrl + (jdbcUrl.contains("?") ? "&" : "?") + "sslmode=require";
        return new ParsedDatasource(withSsl, "", "");
    }

    private static String ensureSslMode(String query) {
        if (query == null || query.isBlank()) {
            return "sslmode=require";
        }
        String lower = query.toLowerCase(Locale.ROOT);
        if (lower.contains("sslmode=")) {
            return query;
        }
        return query + "&sslmode=require";
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String envOrProp(String key) {
        String env = System.getenv(key);
        if (hasText(env)) {
            return env;
        }
        return System.getProperty(key);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    record ParsedDatasource(String jdbcUrl, String username, String password) {
    }
}
