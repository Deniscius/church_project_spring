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
     * Aligné par défaut sur H-6 avant la célébration.
     */
    private int unpaidCancelHoursBefore = 6;

    /** Cron du balayage des impayés (défaut : toutes les 15 minutes). */
    private String unpaidCancelCron = "0 */15 * * * *";

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

    /** Rappels avant célébration (demandes payées / validées) : J-1 et H-2. */
    private boolean celebrationReminderEnabled = true;

    /** Cron des rappels de célébration (défaut : toutes les 15 minutes). */
    private String celebrationReminderCron = "0 */15 * * * *";

    /**
     * Passage automatique en « célébrée » après la messe
     * (et TERMINEE quand toutes les dates le sont).
     */
    private boolean celebrationAutoCompleteEnabled = true;

    /**
     * Délai de sécurité après l'heure théorique de célébration avant de marquer
     * automatiquement le créneau comme célébré. Par défaut : 5 heures.
     */
    private int celebrationAutoCompleteDelayHours = 5;

    /** Cron du balayage auto-célébration (défaut : toutes les 5 minutes). */
    private String celebrationAutoCompleteCron = "0 */5 * * * *";
}
