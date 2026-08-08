package com.eyram.dev.church_project_spring.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@Getter
@Setter
@ConfigurationProperties(prefix = "platform")
public class PlatformBillingProperties {

    /**
     * Commission plateforme (développeur), payée en plus par le fidèle.
     * Ex. 5.00 = 5 % du prix de la messe.
     */
    private BigDecimal commissionPercent = new BigDecimal("5.00");

    /** Commission fixe additionnelle (XOF). */
    private int commissionFixedXof = 0;

    /**
     * Tolérance après l'échéance : la paroisse reste opérationnelle mais son
     * abonnement est marqué expiré, le temps qu'elle règle le renouvellement.
     * Passé ce délai, l'accès est coupé.
     */
    private int subscriptionGraceDays = 7;

    /** Fenêtre d'alerte « échéance proche » présentée au comptable. */
    private int subscriptionWarningDays = 15;

    /** Passage quotidien qui fait tomber les échéances. Lu par {@code SubscriptionExpiryJob}. */
    private String subscriptionSweepCron = "0 15 2 * * *";
}
