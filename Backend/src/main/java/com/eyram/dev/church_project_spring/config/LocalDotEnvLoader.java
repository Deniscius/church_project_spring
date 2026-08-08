package com.eyram.dev.church_project_spring.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Charge {@code Backend/.env} en variables d'environnement / propriétés système
 * pour le développement local, sans jamais versionner de secrets.
 * N'écrase pas une variable déjà définie (IDE, CI, shell).
 */
public final class LocalDotEnvLoader {

    private LocalDotEnvLoader() {
    }

    public static void loadIfPresent() {
        Path envFile = resolveEnvFile();
        if (envFile == null || !Files.isRegularFile(envFile)) {
            return;
        }
        try {
            for (String raw : Files.readAllLines(envFile, StandardCharsets.UTF_8)) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = line.substring(0, eq).trim();
                String value = unquote(line.substring(eq + 1).trim());
                if (key.isEmpty()) {
                    continue;
                }
                if (System.getenv(key) != null) {
                    continue;
                }
                if (System.getProperty(key) != null) {
                    continue;
                }
                System.setProperty(key, value);
                if ("SPRING_PROFILES_ACTIVE".equals(key)) {
                    System.setProperty("spring.profiles.active", value);
                }
            }
        } catch (IOException ignored) {
            // Démarrage possible via variables déjà exportées.
        }
    }

    private static Path resolveEnvFile() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        Path[] candidates = {
                cwd.resolve(".env"),
                cwd.resolve("Backend").resolve(".env"),
                cwd.getParent() != null ? cwd.getParent().resolve(".env") : null,
        };
        for (Path candidate : candidates) {
            if (candidate != null && Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    /** Utile pour logs de diagnostic sans fuite de secrets. */
    public static boolean looksLikeDevProfile() {
        String profile = System.getProperty("SPRING_PROFILES_ACTIVE",
                System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", ""));
        return profile.toLowerCase(Locale.ROOT).contains("dev");
    }
}
