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

import java.util.Locale;

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
                || !StringUtils.hasText(seedProperties.getPassword())
                || !StringUtils.hasText(seedProperties.getNom())
                || !StringUtils.hasText(seedProperties.getPrenom())) {
            log.warn("Seeder admin : configuration incomplète. Aucun compte créé.");
            return;
        }

        String username = seedProperties.getUsername().strip().toLowerCase(Locale.ROOT);
        if (username.length() < 3
                || username.length() > 100
                || !username.matches("^[a-z0-9._-]+$")) {
            log.error("Seeder admin : nom d'utilisateur invalide. Aucun compte créé.");
            return;
        }
        if (seedProperties.getPassword().length() < 8
                || seedProperties.getPassword().length() > 200) {
            log.error("Seeder admin : le mot de passe doit contenir entre 8 et 200 caractères.");
            return;
        }
        if (seedProperties.getRole() != UserRole.SUPER_ADMIN) {
            log.error("Seeder admin : seul le rôle SUPER_ADMIN est autorisé pour le compte initial.");
            return;
        }

        User admin = new User();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(seedProperties.getPassword()));
        admin.setNom(seedProperties.getNom().strip().replaceAll("\\s+", " "));
        admin.setPrenom(seedProperties.getPrenom().strip().replaceAll("\\s+", " "));
        admin.setIsGlobal(true);
        admin.setIsActive(true);
        admin.setRole(UserRole.SUPER_ADMIN);
        admin.setStatusDel(false);

        userRepository.save(admin);

        log.warn(
                "Compte seed initial créé (username='{}', role='{}'). Changez le mot de passe et désactivez app.admin-seed en production.",
                admin.getUsername(),
                admin.getRole());
    }
}
