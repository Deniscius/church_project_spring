package com.eyram.dev.church_project_spring.bootstrap;

import com.eyram.dev.church_project_spring.config.AdminSeedProperties;
import com.eyram.dev.church_project_spring.entities.User;
import com.eyram.dev.church_project_spring.enums.UserRole;
import com.eyram.dev.church_project_spring.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Order
public class AdminUserSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminSeedProperties seedProperties;

    public AdminUserSeeder(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           AdminSeedProperties seedProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedProperties = seedProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!seedProperties.isEnabled()) {
            log.debug("app.admin-seed.enabled=false : aucun compte patient zéro créé.");
            return;
        }

        if (userRepository.countByStatusDelFalse() > 0) {
            log.debug("Des utilisateurs existent déjà : le seeder patient zéro est ignoré.");
            return;
        }

        if (!StringUtils.hasText(seedProperties.getUsername())
                || !StringUtils.hasText(seedProperties.getPassword())) {
            log.warn("Seeder admin : username ou mot de passe vide. Aucun compte créé.");
            return;
        }

        User admin = new User();
        admin.setUsername(seedProperties.getUsername().trim());
        admin.setPassword(passwordEncoder.encode(seedProperties.getPassword()));
        admin.setNom(seedProperties.getNom());
        admin.setPrenom(seedProperties.getPrenom());
        admin.setIsGlobal(true);
        admin.setIsActive(true);
        admin.setRole(UserRole.ADMIN);

        userRepository.save(admin);

        log.warn(
                "Compte administrateur initial créé (username='{}'). Changez le mot de passe et désactivez app.admin-seed en production.",
                admin.getUsername());
    }
}
