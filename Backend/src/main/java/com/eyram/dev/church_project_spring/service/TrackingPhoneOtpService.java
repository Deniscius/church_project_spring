package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.response.TrackingByPhoneChallengeResponse;
import com.eyram.dev.church_project_spring.DTO.response.TrackingByPhoneResponse;
import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.repositories.DemandeRepository;
import com.eyram.dev.church_project_spring.service.mail.AppMailService;
import com.eyram.dev.church_project_spring.utils.PiiMasking;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Suivi par téléphone : OTP e-mail si une adresse est liée, sinon codes directs.
 */
@Service
@RequiredArgsConstructor
public class TrackingPhoneOtpService {

    private static final int OTP_TTL_SECONDS = 600;
    private static final int MAX_ATTEMPTS = 5;

    private final DemandeRepository demandeRepository;
    private final AppMailService mailService;
    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<String, Challenge> challenges = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public TrackingByPhoneChallengeResponse requestOtp(String telephoneRaw) {
        Set<String> variants = phoneLookupVariants(telephoneRaw);
        if (variants.isEmpty()) {
            throw new BusinessRuleException("Numéro de téléphone invalide.");
        }
        List<Demande> hits = demandeRepository.findByTelFideleIn(variants, PageRequest.of(0, 10));
        if (hits.isEmpty()) {
            // Anti-énumération : même message générique.
            throw new BusinessRuleException(
                    "Aucune demande récupérable automatiquement pour ce numéro. Utilisez votre code de suivi."
            );
        }
        List<String> codes = hits.stream()
                .map(Demande::getCodeSuivie)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (codes.isEmpty()) {
            throw new BusinessRuleException(
                    "Aucune demande récupérable automatiquement pour ce numéro. Utilisez votre code de suivi."
            );
        }

        String email = hits.stream()
                .map(Demande::getEmailFidele)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(e -> e.toLowerCase(Locale.ROOT))
                .findFirst()
                .orElse(null);

        // Cas fréquent : dépôt sans e-mail → renvoyer les codes liés au téléphone.
        if (email == null) {
            return new TrackingByPhoneChallengeResponse(
                    null,
                    0,
                    "Voici les codes de suivi associés à ce numéro.",
                    codes
            );
        }

        String phoneKey = canonicalPhoneKey(variants);
        String code = String.format("%06d", random.nextInt(1_000_000));
        challenges.put(phoneKey, new Challenge(
                code,
                Instant.now().plusSeconds(OTP_TTL_SECONDS),
                0,
                codes,
                email
        ));

        // Sync : en prod fail-closed SMTP doit faire échouer la requête HTTP.
        mailService.sendText(
                email,
                "Code de suivi — Missanye",
                """
                        Bonjour,

                        Voici votre code pour retrouver vos demandes Missanye : %s

                        Il expire dans 10 minutes. Si vous n'êtes pas à l'origine de cette demande, ignorez ce message.

                        — Missanye · www.missanye.com
                        """.formatted(code)
        );

        return new TrackingByPhoneChallengeResponse(
                PiiMasking.maskEmail(email),
                OTP_TTL_SECONDS,
                "Un code a été envoyé à l'adresse e-mail associée à ce numéro.",
                List.of()
        );
    }

    public TrackingByPhoneResponse verifyOtp(String telephoneRaw, String codeRaw) {
        Set<String> variants = phoneLookupVariants(telephoneRaw);
        if (variants.isEmpty()) {
            throw new BusinessRuleException("Numéro de téléphone invalide.");
        }
        String phoneKey = canonicalPhoneKey(variants);
        Challenge entry = challenges.get(phoneKey);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            challenges.remove(phoneKey);
            throw new BusinessRuleException("Code expiré ou inexistant. Demandez un nouveau code.");
        }
        if (entry.attempts() >= MAX_ATTEMPTS) {
            challenges.remove(phoneKey);
            throw new BusinessRuleException("Trop de tentatives. Demandez un nouveau code.");
        }
        String code = codeRaw == null ? "" : codeRaw.strip();
        if (!entry.code().equals(code)) {
            challenges.put(phoneKey, new Challenge(
                    entry.code(), entry.expiresAt(), entry.attempts() + 1, entry.codes(), entry.email()
            ));
            throw new BusinessRuleException("Code incorrect.");
        }
        challenges.remove(phoneKey);
        return new TrackingByPhoneResponse(entry.codes(), entry.codes().size());
    }

    @Scheduled(fixedDelayString = "${app.inscription.otp-purge-ms:300000}")
    public void purgeExpired() {
        Instant now = Instant.now();
        challenges.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
    }

    private static String canonicalPhoneKey(Set<String> variants) {
        return variants.stream()
                .filter(v -> v.startsWith("+"))
                .findFirst()
                .orElseGet(() -> variants.iterator().next())
                .toLowerCase(Locale.ROOT);
    }

    static Set<String> phoneLookupVariants(String raw) {
        if (raw == null) {
            return Set.of();
        }
        String compact = raw.trim().replaceAll("[\\s.\\-()]", "");
        if (compact.length() < 8) {
            return Set.of();
        }
        Set<String> out = new LinkedHashSet<>();
        out.add(compact);
        if (compact.startsWith("00") && compact.length() > 4) {
            out.add("+" + compact.substring(2));
        }
        if (compact.startsWith("+")) {
            out.add(compact.substring(1));
        } else if (compact.matches("\\d{8,15}")) {
            out.add("+" + compact);
        }
        return out;
    }

    private record Challenge(
            String code,
            Instant expiresAt,
            int attempts,
            List<String> codes,
            String email
    ) {
    }
}
