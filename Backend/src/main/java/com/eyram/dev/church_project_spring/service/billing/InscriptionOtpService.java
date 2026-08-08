package com.eyram.dev.church_project_spring.service.billing;

import com.eyram.dev.church_project_spring.repositories.ParoisseInscriptionRepository;
import com.eyram.dev.church_project_spring.repositories.UserRepository;
import com.eyram.dev.church_project_spring.service.ProfessionalEmailService;
import com.eyram.dev.church_project_spring.service.mail.AppMailService;
import com.eyram.dev.church_project_spring.utils.BusinessCodeGenerator;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OTP e-mail personnel pour l'inscription admin paroisse, puis génération
 * d'identifiants communiqués une fois le code validé.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InscriptionOtpService {

    private static final int OTP_TTL_SECONDS = 600;
    private static final int PROOF_TTL_SECONDS = 1800;
    private static final int MAX_ATTEMPTS = 5;

    private final AppMailService mailService;
    private final ProfessionalEmailService professionalEmailService;
    private final UserRepository userRepository;
    private final ParoisseInscriptionRepository inscriptionRepository;
    private final SecureRandom random = new SecureRandom();

    private final ConcurrentHashMap<String, OtpEntry> otps = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ProofEntry> proofs = new ConcurrentHashMap<>();

    public Map<String, Object> sendOtp(String emailRaw) {
        String email = normalizeEmail(emailRaw);
        String code = String.format("%06d", random.nextInt(1_000_000));
        otps.put(email, new OtpEntry(code, Instant.now().plusSeconds(OTP_TTL_SECONDS), 0));

        mailService.sendTextAsync(
                email,
                "Code de vérification — Missanye",
                """
                        Bonjour,

                        Votre code de vérification pour l'inscription paroisse Missanye est : %s

                        Il expire dans 10 minutes. Si vous n'êtes pas à l'origine de cette demande, ignorez ce message.

                        — Missanye · www.missanye.com
                        """.formatted(code)
        );

        return Map.of(
                "email", email,
                "expiresInSeconds", OTP_TTL_SECONDS,
                "message", "Un code à 6 chiffres a été envoyé à votre adresse e-mail."
        );
    }

    public Map<String, Object> verifyOtp(
            String emailRaw,
            String codeRaw,
            String prenom,
            String nom,
            String nomParoisse
    ) {
        String email = normalizeEmail(emailRaw);
        String code = codeRaw == null ? "" : codeRaw.strip();
        OtpEntry entry = otps.get(email);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            otps.remove(email);
            throw new BusinessRuleException("Code expiré ou inexistant. Demandez un nouveau code.");
        }
        if (entry.attempts() >= MAX_ATTEMPTS) {
            otps.remove(email);
            throw new BusinessRuleException("Trop de tentatives. Demandez un nouveau code.");
        }
        if (!entry.code().equals(code)) {
            otps.put(email, new OtpEntry(entry.code(), entry.expiresAt(), entry.attempts() + 1));
            throw new BusinessRuleException("Code incorrect.");
        }
        otps.remove(email);

        String username = uniqueUsername(prenom, nom, nomParoisse);
        String password = generatePassword();
        String proof = UUID.randomUUID().toString();
        proofs.put(proof, new ProofEntry(email, username, password, Instant.now().plusSeconds(PROOF_TTL_SECONDS)));

        mailService.sendTextAsync(
                email,
                "Vos identifiants — Missanye",
                """
                        Bonjour %s %s,

                        Votre adresse e-mail a été vérifiée. Voici les identifiants de votre futur compte administrateur de paroisse :

                        Identifiant : %s
                        Mot de passe temporaire : %s

                        Conservez-les précieusement. Après validation de votre dossier et activation de l'abonnement, vous pourrez aussi vous connecter avec votre e-mail professionnel paroisse (généré automatiquement).

                        — Missanye · www.missanye.com
                        """.formatted(
                        prenom == null ? "" : prenom.trim(),
                        nom == null ? "" : nom.trim(),
                        username,
                        password
                )
        );

        return Map.of(
                "email", email,
                "otpProof", proof,
                "adminUsername", username,
                "adminPassword", password,
                "expiresInSeconds", PROOF_TTL_SECONDS,
                "message", "E-mail vérifié. Identifiants générés et envoyés à votre adresse personnelle."
        );
    }

    public ProofEntry requireValidProof(String emailRaw, String proof, String expectedUsername) {
        String email = normalizeEmail(emailRaw);
        if (!StringUtils.hasText(proof)) {
            throw new BusinessRuleException("Vérification e-mail requise (OTP).");
        }
        ProofEntry entry = proofs.get(proof.strip());
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            proofs.remove(proof);
            throw new BusinessRuleException("Session de vérification expirée. Recommencez la validation OTP.");
        }
        if (!entry.email().equals(email)) {
            throw new BusinessRuleException("L'e-mail ne correspond pas à la vérification OTP.");
        }
        if (StringUtils.hasText(expectedUsername)
                && !entry.username().equalsIgnoreCase(expectedUsername.strip())) {
            throw new BusinessRuleException("Identifiant incohérent avec la vérification OTP.");
        }
        return entry;
    }

    public void consumeProof(String proof) {
        if (StringUtils.hasText(proof)) {
            proofs.remove(proof.strip());
        }
    }

    /** Purge des OTP / preuves expirés (évite la croissance mémoire indéfinie). */
    @Scheduled(fixedDelayString = "${app.inscription.otp-purge-ms:300000}")
    public void purgeExpired() {
        Instant now = Instant.now();
        int otpRemoved = 0;
        int proofRemoved = 0;
        for (var entry : otps.entrySet()) {
            if (entry.getValue().expiresAt().isBefore(now)) {
                otps.remove(entry.getKey(), entry.getValue());
                otpRemoved++;
            }
        }
        for (var entry : proofs.entrySet()) {
            if (entry.getValue().expiresAt().isBefore(now)) {
                proofs.remove(entry.getKey(), entry.getValue());
                proofRemoved++;
            }
        }
        if (otpRemoved > 0 || proofRemoved > 0) {
            log.debug("Purge OTP: {} codes, {} preuves expirés", otpRemoved, proofRemoved);
        }
    }

    /**
     * Identifiant lisible, centré paroisse :
     * {@code admin.saint-joseph}, sinon {@code admin.sj}, avec prénom si collision.
     */
    private String uniqueUsername(String prenom, String nom, String nomParoisse) {
        String parishSlug = StringUtils.hasText(nomParoisse)
                ? professionalEmailService.slugify(nomParoisse)
                : "";
        String initials = StringUtils.hasText(nomParoisse)
                ? BusinessCodeGenerator.parishInitials(nomParoisse).toLowerCase(Locale.ROOT)
                : "";
        String prenomSlug = professionalEmailService.slugify(prenom);
        String nomSlug = professionalEmailService.slugify(nom);

        String base;
        if (StringUtils.hasText(parishSlug) && !"paroisse".equals(parishSlug)) {
            base = "admin." + parishSlug;
        } else if (StringUtils.hasText(initials)) {
            base = "admin." + initials;
        } else if (StringUtils.hasText(prenomSlug) && StringUtils.hasText(nomSlug)
                && !"paroisse".equals(prenomSlug) && !"paroisse".equals(nomSlug)) {
            base = prenomSlug + "." + nomSlug;
        } else {
            base = "admin.paroisse";
        }

        if (base.length() > 70) {
            base = StringUtils.hasText(initials) ? "admin." + initials : base.substring(0, 70);
        }

        String candidate = base;
        int i = 2;
        while (userRepository.existsByUsernameIgnoreCaseAndStatusDelFalse(candidate)
                || inscriptionRepository.existsByAdminUsernameIgnoreCaseAndStatusDelFalse(candidate)) {
            // Collision : greffer le prénom puis un compteur
            if (i == 2 && StringUtils.hasText(prenomSlug) && !"paroisse".equals(prenomSlug)
                    && !base.contains("." + prenomSlug)) {
                candidate = base + "." + prenomSlug;
            } else {
                candidate = base + i;
            }
            i++;
            if (i > 99) {
                candidate = base + "-" + UUID.randomUUID().toString().substring(0, 4);
                break;
            }
        }
        return candidate.toLowerCase(Locale.ROOT);
    }

    private String generatePassword() {
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }

    private static String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BusinessRuleException("E-mail personnel requis.");
        }
        String normalized = email.strip().toLowerCase(Locale.ROOT);
        if (!normalized.contains("@") || normalized.length() < 5) {
            throw new BusinessRuleException("E-mail personnel invalide.");
        }
        return normalized;
    }

    public record OtpEntry(String code, Instant expiresAt, int attempts) {
    }

    public record ProofEntry(String email, String username, String password, Instant expiresAt) {
    }
}
