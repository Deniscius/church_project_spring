package com.eyram.dev.church_project_spring.config;

import com.eyram.dev.church_project_spring.enums.FeePayer;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@Getter
@Setter
@ConfigurationProperties(prefix = "fedapay")
public class FedaPayProperties {

    /**
     * Désactive les appels API (checkout échoue clairement si false).
     */
    private boolean enabled = false;

    /** sandbox | live */
    private String environment = "sandbox";

    private String secretKey = "";

    /** Secret du point de terminaison webhook (wh_...). */
    private String webhookSecret = "";

    /**
     * Base URL front pour callback_url FedaPay (ex. https://app.example.com/paiement).
     * FedaPay append ?id=...&status=...
     */
    private String callbackBaseUrl = "http://localhost:5173/paiement";

    private String currency = "XOF";

    /** Code pays ISO alpha-2 pour le téléphone client (tg, bj, ci...). */
    private String customerCountry = "tg";

    private int webhookToleranceSeconds = 300;

    /** Timeout connexion HTTP vers l'API FedaPay (ms). */
    private int connectTimeoutMs = 3000;

    /** Timeout lecture HTTP vers l'API FedaPay (ms). */
    private int readTimeoutMs = 8000;

    private Fees fees = new Fees();

    public String apiBaseUrl() {
        if ("live".equalsIgnoreCase(environment)) {
            return "https://api.fedapay.com/v1";
        }
        return "https://sandbox-api.fedapay.com/v1";
    }

    @Getter
    @Setter
    public static class Fees {
        private FeePayer payer = FeePayer.CUSTOMER;
        private BigDecimal tmoneyPercent = new BigDecimal("1.80");
        private BigDecimal floozPercent = new BigDecimal("1.80");
        private BigDecimal cartePercent = new BigDecimal("3.60");
        /** Frais fixes additionnels (XOF), souvent 0 sur collectes. */
        private int fixedXof = 0;
    }
}
