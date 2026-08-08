package com.eyram.dev.church_project_spring.config;

import com.eyram.dev.church_project_spring.enums.UserRole;
import com.eyram.dev.church_project_spring.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Les opérations financières sont réservées au COMPTABLE : sans compte comptable
 * actif, aucune activation d'abonnement ni reversement n'est possible. On alerte
 * au démarrage plutôt que de laisser découvrir le blocage en production.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformTeamStartupCheck implements ApplicationRunner {

    private final UserRepository userRepository;

    @Override
    public void run(ApplicationArguments args) {
        long comptables = userRepository
                .countByStatusDelFalseAndIsActiveTrueAndIsGlobalTrueAndRole(UserRole.COMPTABLE);

        if (comptables == 0) {
            log.warn(
                    "Aucun COMPTABLE actif : les activations d'abonnement et les reversements "
                            + "resteront bloqués tant que le SUPER_ADMIN n'aura pas créé ce compte."
            );
        }
    }
}
