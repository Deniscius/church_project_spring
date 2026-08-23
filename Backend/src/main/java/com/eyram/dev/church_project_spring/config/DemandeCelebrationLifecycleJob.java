package com.eyram.dev.church_project_spring.config;

import com.eyram.dev.church_project_spring.context.TenantContext;

import com.eyram.dev.church_project_spring.service.DemandeCelebrationLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Rappels J-1 / H-2 avant célébration, et passage auto en « célébrée »
 * après le délai de sécurité configuré suivant l'heure prévue.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemandeCelebrationLifecycleJob implements ApplicationRunner {

    private final DemandeCelebrationLifecycleService celebrationLifecycleService;
    private final DemandePaymentProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        if (properties.isCelebrationReminderEnabled()) {
            remind("démarrage");
        }
        if (properties.isCelebrationAutoCompleteEnabled()) {
            complete("démarrage");
        }
    }

    @Scheduled(cron = "${app.demande.celebration-reminder-cron:0 */15 * * * *}")
    public void scheduledReminders() {
        if (properties.isCelebrationReminderEnabled()) {
            remind("planifié");
        }
    }

    @Scheduled(cron = "${app.demande.celebration-auto-complete-cron:0 */5 * * * *}")
    public void scheduledAutoComplete() {
        if (properties.isCelebrationAutoCompleteEnabled()) {
            complete("planifié");
        }
    }

    private void remind(String origine) {
        try {
            int sent = TenantContext.withoutTenant(celebrationLifecycleService::sendUpcomingCelebrationReminders);
            if (sent > 0) {
                log.info("Rappels célébration {} : {} e-mail(s) fidèle envoyé(s)", origine, sent);
            }
        } catch (Exception exception) {
            log.error("Échec des rappels célébration ({})", origine, exception);
        }
    }

    private void complete(String origine) {
        try {
            int marked = TenantContext.withoutTenant(celebrationLifecycleService::autoCompletePastCelebrations);
            if (marked > 0) {
                log.info("Auto-célébration {} : {} créneau(x) marqué(s)", origine, marked);
            }
        } catch (Exception exception) {
            log.error("Échec de l'auto-célébration ({})", origine, exception);
        }
    }
}
