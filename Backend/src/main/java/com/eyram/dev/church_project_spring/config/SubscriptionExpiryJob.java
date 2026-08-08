package com.eyram.dev.church_project_spring.config;

import com.eyram.dev.church_project_spring.service.billing.SubscriptionBillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sans passage régulier, un abonnement échu resterait « ACTIF » en base et la
 * paroisse conserverait son accès indéfiniment. Le balayage tourne chaque nuit
 * et une fois au démarrage, pour rattraper les échéances tombées pendant un
 * arrêt de l'application.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionExpiryJob implements ApplicationRunner {

    private final SubscriptionBillingService subscriptionBillingService;

    @Override
    public void run(ApplicationArguments args) {
        sweep("démarrage");
    }

    @Scheduled(cron = "${platform.subscription-sweep-cron:0 15 2 * * *}")
    public void scheduledSweep() {
        sweep("planifié");
    }

    private void sweep(String origine) {
        try {
            int changed = subscriptionBillingService.sweepEcheances();
            if (changed > 0) {
                log.info("Balayage {} des échéances : {} état(s) mis à jour", origine, changed);
            }
        } catch (Exception exception) {
            log.error("Échec du balayage {} des échéances d'abonnement", origine, exception);
        }
    }
}
