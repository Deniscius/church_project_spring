package com.eyram.dev.church_project_spring.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Règles de paiement des demandes fidèles (hors abonnement paroisse).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.demande")
public class DemandePaymentProperties {

    /**
     * Si true, les demandes encore non payées sont annulées automatiquement
     * lorsque la première célébration approche (voir {@link #unpaidCancelHoursBefore}).
     */
    private boolean unpaidAutoCancelEnabled = true;

    /**
     * Fenêtre avant la première célébration : une demande non payée dont la
     * célébration commence dans moins de N heures (ou déjà commencée) est annulée.
     * Aligné par défaut sur le délai minimum de dépôt (24 h).
     */
    private int unpaidCancelHoursBefore = 24;

    /** Cron du balayage des impayés (défaut : toutes les heures). */
    private String unpaidCancelCron = "0 20 * * * *";

    /**
     * Relances e-mail pour les demandes non payées dont la 1ère célébration
     * approche (fenêtre {@link #unpaidReminderDaysBefore}).
     */
    private boolean unpaidReminderEnabled = true;

    /**
     * Nombre de jours avant la 1ère célébration à partir duquel on relance
     * le fidèle (et on alerte le dashboard paroisse).
     */
    private int unpaidReminderDaysBefore = 3;

    /** Cron des rappels (défaut : toutes les 6 heures). */
    private String unpaidReminderCron = "0 0 */6 * * *";

    /**
     * URL publique du front (liens de suivi dans les e-mails).
     * Ex. https://www.missanye.com
     */
    private String publicBaseUrl = "http://localhost:5173";
}
