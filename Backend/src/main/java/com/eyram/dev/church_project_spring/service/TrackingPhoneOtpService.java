package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.response.TrackingByPhoneChallengeResponse;
import com.eyram.dev.church_project_spring.DTO.response.TrackingByPhoneItemResponse;
import com.eyram.dev.church_project_spring.DTO.response.TrackingByPhoneResponse;
import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.entities.DemandeDate;
import com.eyram.dev.church_project_spring.repositories.DemandeDateRepository;
import com.eyram.dev.church_project_spring.repositories.DemandeRepository;
import com.eyram.dev.church_project_spring.service.mail.AppMailService;
import com.eyram.dev.church_project_spring.utils.PiiMasking;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Suivi par téléphone : OTP e-mail si une adresse est liée, sinon résultats directs.
 */
@Service
@RequiredArgsConstructor
public class TrackingPhoneOtpService {

    private static final int OTP_TTL_SECONDS = 600;
    private static final int MAX_ATTEMPTS = 5;

    private final DemandeRepository demandeRepository;
    private final DemandeDateRepository demandeDateRepository;
    private final AppMailService mailService;
    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<String, Challenge> challenges = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public TrackingByPhoneChallengeResponse requestOtp(String telephoneRaw) {
        Set<String> variants = phoneLookupVariants(telephoneRaw);
        if (variants.isEmpty()) {
            throw new BusinessRuleException("Numéro de téléphone invalide.");
        }

        List<Demande> hits = demandeRepository.findAllByTelFideleInChronological(variants);
        if (hits.isEmpty()) {
            // Anti-énumération : même message générique.
            throw new BusinessRuleException(
                    "Aucune demande récupérable automatiquement pour ce numéro. Utilisez votre code de suivi."
            );
        }

        List<TrackingByPhoneItemResponse> demandes = buildTrackingItems(hits);
        List<String> codes = demandes.stream()
                .map(TrackingByPhoneItemResponse::codeSuivie)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (codes.isEmpty()) {
            throw new BusinessRuleException(
                    "Aucune demande récupérable automatiquement pour ce numéro. Utilisez votre code de suivi."
            );
        }

        // Utiliser l'adresse la plus récemment renseignée pour ce numéro.
        String email = hits.stream()
                .map(Demande::getEmailFidele)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(e -> e.toLowerCase(Locale.ROOT))
                .reduce((first, second) -> second)
                .orElse(null);

        // Cas fréquent : dépôt sans e-mail → renvoyer directement la liste minimale.
        if (email == null) {
            return new TrackingByPhoneChallengeResponse(
                    null,
                    0,
                    "Voici les demandes associées à ce numéro.",
                    codes,
                    demandes
            );
        }

        String phoneKey = canonicalPhoneKey(variants);
        String code = String.format("%06d", random.nextInt(1_000_000));
        challenges.put(phoneKey, new Challenge(
                code,
                Instant.now().plusSeconds(OTP_TTL_SECONDS),
                0,
                codes,
                demandes,
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
                List.of(),
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
                    entry.code(),
                    entry.expiresAt(),
                    entry.attempts() + 1,
                    entry.codes(),
                    entry.demandes(),
                    entry.email()
            ));
            throw new BusinessRuleException("Code incorrect.");
        }
        challenges.remove(phoneKey);
        return new TrackingByPhoneResponse(
                entry.codes(),
                entry.demandes(),
                entry.demandes().size()
        );
    }

    private List<TrackingByPhoneItemResponse> buildTrackingItems(List<Demande> hits) {
        List<Long> demandeIds = hits.stream()
                .map(Demande::getId)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, List<LocalDate>> datesByDemande = demandeIds.isEmpty()
                ? Map.of()
                : demandeDateRepository.findByDemande_IdInAndStatusDelFalseOrderByOrdreAsc(demandeIds)
                .stream()
                .collect(Collectors.groupingBy(
                        row -> row.getDemande().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(DemandeDate::getDateCelebration, Collectors.toList())
                ));

        return hits.stream()
                .map(demande -> new TrackingByPhoneItemResponse(
                        demande.getCodeSuivie(),
                        demande.getCreatedAt(),
                        demande.getParoisse() != null ? demande.getParoisse().getNom() : null,
                        demande.getTypeDemande() != null ? demande.getTypeDemande().getLibelle() : null,
                        demande.getStatutDemande(),
                        demande.getStatutPaiement(),
                        datesByDemande.getOrDefault(demande.getId(), List.of())
                ))
                .toList();
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
            List<TrackingByPhoneItemResponse> demandes,
            String email
    ) {
    }
}
