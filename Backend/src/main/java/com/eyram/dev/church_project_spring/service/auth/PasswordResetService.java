package com.eyram.dev.church_project_spring.service.auth;

import com.eyram.dev.church_project_spring.DTO.request.ForgotPasswordRequest;
import com.eyram.dev.church_project_spring.DTO.request.ResetPasswordRequest;
import com.eyram.dev.church_project_spring.config.CacheConfig;
import com.eyram.dev.church_project_spring.entities.PasswordResetToken;
import com.eyram.dev.church_project_spring.entities.User;
import com.eyram.dev.church_project_spring.repositories.PasswordResetTokenRepository;
import com.eyram.dev.church_project_spring.repositories.UserRepository;
import com.eyram.dev.church_project_spring.service.mail.AppMailService;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Réinitialisation de mot de passe sécurisée :
 * <ul>
 *   <li>Réponse générique (pas d'énumération de comptes)</li>
 *   <li>Jeton aléatoire stocké uniquement sous forme de hash SHA-256</li>
 *   <li>Expiration courte, usage unique, invalidation des jetons antérieurs</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int TOKEN_BYTES = 32;
    private static final int EXPIRY_MINUTES = 30;
    private static final String GENERIC_MESSAGE =
            "Si un compte correspond, un e-mail de réinitialisation a été envoyé. "
                    + "Vérifiez votre boîte de réception (et les indésirables).";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppMailService appMailService;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.demande.public-base-url:http://localhost:5173}")
    private String publicBaseUrl;

    @Transactional
    public Map<String, String> requestReset(ForgotPasswordRequest request, String clientIp) {
        String raw = request.usernameOrEmail() == null ? "" : request.usernameOrEmail().trim();
        if (!StringUtils.hasText(raw)) {
            return Map.of("message", GENERIC_MESSAGE);
        }

        Optional<User> userOpt = findActiveUser(raw);
        if (userOpt.isEmpty()) {
            log.info("Forgot-password : aucun compte pour « {} »", mask(raw));
            return Map.of("message", GENERIC_MESSAGE);
        }

        User user = userOpt.get();
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            return Map.of("message", GENERIC_MESSAGE);
        }
        if (!StringUtils.hasText(user.getEmail())) {
            log.warn("Forgot-password : utilisateur {} sans e-mail", user.getUsername());
            return Map.of("message", GENERIC_MESSAGE);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        tokenRepository.invalidateUnusedForUser(user, now);

        String rawToken = generateRawToken();
        PasswordResetToken entity = new PasswordResetToken();
        entity.setUser(user);
        entity.setTokenHash(sha256(rawToken));
        entity.setExpiresAt(now.plusMinutes(EXPIRY_MINUTES));
        entity.setCreatedAt(now);
        entity.setRequestIp(truncateIp(clientIp));
        tokenRepository.save(entity);

        String resetUrl = publicBaseUrl.replaceAll("/+$", "")
                + "/admin/reset-password?token=" + rawToken;
        String subject = "Réinitialisation de votre mot de passe — Missanye";
        String body = """
                Bonjour %s,

                Une demande de réinitialisation a été faite pour votre compte Missanye (%s).

                Lien valable %d minutes :
                %s

                Si vous n'êtes pas à l'origine de cette demande, ignorez cet e-mail.

                — Missanye
                """.formatted(
                displayName(user),
                user.getUsername(),
                EXPIRY_MINUTES,
                resetUrl
        );
        appMailService.sendTextAsync(user.getEmail().trim(), subject, body);
        log.info("Forgot-password : jeton émis pour {}", user.getUsername());

        return Map.of("message", GENERIC_MESSAGE);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.AUTH_USER_DETAILS, allEntries = true)
    public Map<String, String> resetPassword(ResetPasswordRequest request) {
        String rawToken = request.token() == null ? "" : request.token().trim();
        String newPassword = request.newPassword();

        if (!StringUtils.hasText(rawToken) || !StringUtils.hasText(newPassword)) {
            throw new BusinessRuleException("Jeton ou mot de passe manquant");
        }
        if (newPassword.length() < 8 || newPassword.length() > 200) {
            throw new BusinessRuleException("Le mot de passe doit contenir entre 8 et 200 caractères");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        PasswordResetToken token = tokenRepository.findByTokenHashAndUsedAtIsNull(sha256(rawToken))
                .orElseThrow(() -> new BusinessRuleException(
                        "Lien de réinitialisation invalide ou déjà utilisé."
                ));

        if (token.isExpired(now)) {
            token.setUsedAt(now);
            tokenRepository.save(token);
            throw new BusinessRuleException("Ce lien a expiré. Demandez une nouvelle réinitialisation.");
        }

        User user = token.getUser();
        if (user == null || Boolean.TRUE.equals(user.getStatusDel()) || !Boolean.TRUE.equals(user.getIsActive())) {
            throw new BusinessRuleException("Compte indisponible. Contactez le support.");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessRuleException("Le nouveau mot de passe doit différer de l'actuel");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setTokenVersion(nextTokenVersion(user));
        userRepository.save(user);

        token.setUsedAt(now);
        tokenRepository.save(token);
        tokenRepository.invalidateUnusedForUser(user, now);

        log.info("Mot de passe réinitialisé pour {}", user.getUsername());
        return Map.of("message", "Mot de passe mis à jour. Vous pouvez vous connecter.");
    }

    private static long nextTokenVersion(User user) {
        return user.getTokenVersion() == null ? 1L : user.getTokenVersion() + 1L;
    }

    private Optional<User> findActiveUser(String raw) {
        String normalized = raw.toLowerCase(Locale.ROOT);
        return userRepository.findByUsernameIgnoreCaseAndStatusDelFalse(normalized)
                .or(() -> userRepository.findByEmailIgnoreCaseAndStatusDelFalse(normalized));
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    private static String displayName(User user) {
        String name = ((user.getPrenom() != null ? user.getPrenom() : "")
                + " "
                + (user.getNom() != null ? user.getNom() : "")).trim();
        return StringUtils.hasText(name) ? name : user.getUsername();
    }

    private static String truncateIp(String ip) {
        if (!StringUtils.hasText(ip)) return null;
        return ip.length() > 64 ? ip.substring(0, 64) : ip;
    }

    private static String mask(String value) {
        if (value.length() <= 3) return "***";
        return value.substring(0, 2) + "***";
    }
}
