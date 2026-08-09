package com.eyram.dev.church_project_spring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Limites de débit sur les endpoints publics (anti-abus / montée en charge).
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        @DefaultValue("true") boolean enabled,
        /** Requêtes / minute par IP sur les routes publiques sensibles. */
        @DefaultValue("60") int publicPerMinute,
        /** Création de demandes / minute par IP. */
        @DefaultValue("12") int demandeCreatePerMinute,
        /** OTP / forgot-password / suivi téléphone / minute par IP. */
        @DefaultValue("8") int authSensitivePerMinute,
        /** Webhooks FedaPay / minute par IP (réseau FedaPay). */
        @DefaultValue("120") int webhookPerMinute,
        /** Lecture publique par code (demande / facture / PDF) / minute par IP. */
        @DefaultValue("30") int codeLookupPerMinute
) {
}
