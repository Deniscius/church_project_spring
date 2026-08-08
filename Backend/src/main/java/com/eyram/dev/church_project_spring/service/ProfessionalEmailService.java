package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.User;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Adresses professionnelles courtes sur le domaine plateforme :
 * <ul>
 *   <li>Paroisse : {@code {slug}@missanye.com}</li>
 *   <li>Membre : {@code {prenom}.{nom}@missanye.com}</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ProfessionalEmailService {

    private static final int PARISH_SLUG_MAX = 12;
    private static final int PERSON_PART_MAX = 8;
    private static final Set<String> NOISE_WORDS = Set.of(
            "paroisse", "quasi", "quasi-paroisse", "eglise", "église",
            "saint", "sainte", "st", "ste", "apotre", "apôtre", "bienheureux", "bienheureuse"
    );

    private final ParoisseRepository paroisseRepository;
    private final UserRepository userRepository;

    @Value("${app.mail.tenant-domain:missanye.com}")
    private String tenantDomain;

    public String slugify(String raw) {
        return slugify(raw, PARISH_SLUG_MAX);
    }

    public String slugify(String raw, int maxLen) {
        if (!StringUtils.hasText(raw)) {
            return "paroisse";
        }
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (normalized.startsWith("paroisse-")) {
            normalized = normalized.substring("paroisse-".length());
        }
        if (normalized.startsWith("quasi-paroisse-")) {
            normalized = normalized.substring("quasi-paroisse-".length());
        }
        if (!StringUtils.hasText(normalized)) {
            return "paroisse";
        }

        // Garde les mots significatifs (ex. "Maria Auxiliadora / Gbényédzi" → maria-gbenyedzi)
        String compact = Arrays.stream(normalized.split("-"))
                .filter(StringUtils::hasText)
                .filter(part -> !NOISE_WORDS.contains(part))
                .filter(part -> part.length() > 1)
                .collect(Collectors.joining("-"));
        if (!StringUtils.hasText(compact)) {
            compact = normalized;
        }

        if (compact.length() > maxLen) {
            compact = compact.substring(0, maxLen).replaceAll("-+$", "");
        }
        return StringUtils.hasText(compact) ? compact : "paroisse";
    }

    /** Domaine plateforme unique (plus de sous-domaine = adresse trop longue). */
    public String platformDomain() {
        return tenantDomain.trim().toLowerCase(Locale.ROOT);
    }

    /** @deprecated Prefer {@link #platformDomain()} — conservé pour compatibilité. */
    public String parishMailboxDomain(String parishName) {
        return platformDomain();
    }

    public String forParoisse(String parishName) {
        return uniqueEmail(slugify(parishName) + "@" + platformDomain(), this::paroisseEmailTaken);
    }

    public String forParoisse(String parishName, UUID excludeParoissePublicId) {
        return uniqueEmail(
                slugify(parishName) + "@" + platformDomain(),
                email -> paroisseEmailTaken(email, excludeParoissePublicId)
        );
    }

    public String forMembre(String prenom, String nom, String parishName) {
        return uniqueEmail(memberLocal(prenom, nom, parishName) + "@" + platformDomain(), this::userEmailTaken);
    }

    public String forMembre(String prenom, String nom, String parishName, UUID excludeUserPublicId) {
        return uniqueEmail(
                memberLocal(prenom, nom, parishName) + "@" + platformDomain(),
                email -> userEmailTaken(email, excludeUserPublicId)
        );
    }

    private String memberLocal(String prenom, String nom, String parishName) {
        String p = slugify(prenom, PERSON_PART_MAX);
        String n = slugify(nom, PERSON_PART_MAX);
        // Format court : prenom.nom (sans slug paroisse — le tenant est déjà isolé).
        if (StringUtils.hasText(p) && StringUtils.hasText(n) && !"paroisse".equals(p) && !"paroisse".equals(n)) {
            return p + "." + n;
        }
        if (StringUtils.hasText(n) && !"paroisse".equals(n)) {
            return n;
        }
        if (StringUtils.hasText(p) && !"paroisse".equals(p)) {
            return p;
        }
        // Dernier recours : initiale + slug paroisse court.
        String parish = slugify(parishName, 8);
        String initial = StringUtils.hasText(prenom)
                ? slugify(prenom, 1)
                : "u";
        return initial + "." + parish;
    }

    public void assignToParoisse(Paroisse paroisse) {
        if (paroisse == null || !StringUtils.hasText(paroisse.getNom())) {
            return;
        }
        paroisse.setEmail(forParoisse(paroisse.getNom(), paroisse.getPublicId()));
    }

    public void assignToUser(User user, Paroisse paroisse) {
        if (user == null || paroisse == null || !StringUtils.hasText(paroisse.getNom())) {
            return;
        }
        user.setEmail(forMembre(user.getPrenom(), user.getNom(), paroisse.getNom(), user.getPublicId()));
    }

    public boolean isProfessional(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        String domain = platformDomain();
        String lower = email.toLowerCase(Locale.ROOT);
        return lower.endsWith("." + domain) || lower.endsWith("@" + domain);
    }

    private boolean paroisseEmailTaken(String email) {
        return paroisseEmailTaken(email, null);
    }

    private boolean paroisseEmailTaken(String email, UUID excludePublicId) {
        if (excludePublicId == null) {
            return paroisseRepository.existsByEmailIgnoreCaseAndStatusDelFalse(email);
        }
        return paroisseRepository.existsByEmailIgnoreCaseAndStatusDelFalseAndPublicIdNot(email, excludePublicId);
    }

    private boolean userEmailTaken(String email) {
        return userEmailTaken(email, null);
    }

    private boolean userEmailTaken(String email, UUID excludePublicId) {
        if (excludePublicId == null) {
            return userRepository.existsByEmailIgnoreCaseAndStatusDelFalse(email);
        }
        return userRepository.existsByEmailIgnoreCaseAndStatusDelFalseAndPublicIdNot(email, excludePublicId);
    }

    private static String uniqueEmail(String base, Predicate<String> taken) {
        if (!taken.test(base)) {
            return base;
        }
        int at = base.indexOf('@');
        String local = base.substring(0, at);
        String domain = base.substring(at);
        for (int i = 2; i <= 99; i++) {
            String candidate = local + i + domain;
            if (!taken.test(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Impossible de générer un e-mail unique pour : " + base);
    }
}
