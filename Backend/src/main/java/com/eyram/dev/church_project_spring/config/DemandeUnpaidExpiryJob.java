package com.eyram.dev.church_project_spring.config;

import com.eyram.dev.church_project_spring.context.TenantContext;

import com.eyram.dev.church_project_spring.service.DemandeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Demandes fidèles non payées :
 * <ul>
 *   <li>rappels e-mail à partir de J-3 (toutes les 6 h) ;</li>
 *   <li>annulation automatique à l'approche de la célébration (H-6).</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemandeUnpaidExpiryJob implements ApplicationRunner {

    private final DemandeService demandeService;
    private final DemandePaymentProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        if (properties.isUnpaidReminderEnabled()) {
            remind("démarrage");
        }
        if (properties.isUnpaidAutoCancelEnabled()) {
            sweepCancel("démarrage");
        }
    }

    @Scheduled(cron = "${app.demande.unpaid-reminder-cron:0 0 */6 * * *}")
    public void scheduledReminders() {
        if (properties.isUnpaidReminderEnabled()) {
            remind("planifié");
        }
    }

    @Scheduled(cron = "${app.demande.unpaid-cancel-cron:0 20 * * * *}")
    public void scheduledSweep() {
        if (properties.isUnpaidAutoCancelEnabled()) {
            sweepCancel("planifié");
        }
    }

    private void remind(String origine) {
        try {
            int sent = TenantContext.withoutTenant(demandeService::remindUnpaidApproachingCelebrations);
            if (sent > 0) {
                log.info("Rappels {} des impayés : {} e-mail(s) envoyé(s)", origine, sent);
            }
        } catch (Exception exception) {
            log.error("Échec des rappels {} des demandes impayées", origine, exception);
        }
    }

    private void sweepCancel(String origine) {
        try {
            int cancelled = TenantContext.withoutTenant(demandeService::cancelUnpaidApproachingCelebrations);
            if (cancelled > 0) {
                log.info("Balayage {} des impayés : {} demande(s) annulée(s)", origine, cancelled);
            }
        } catch (Exception exception) {
            log.error("Échec du balayage {} des demandes impayées", origine, exception);
        }
    }
}
