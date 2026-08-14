package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.enums.PhoneVerificationPurpose;
import com.eyram.dev.church_project_spring.enums.PhoneVerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Challenge OTP de vérification d'un numéro de téléphone.
 *
 * Sécurité : l'OTP et le jeton de preuve ne sont jamais persistés en clair ;
 * seuls leurs hash SHA-256 sont stockés.
 */
@Entity
@Table(
        name = "phone_verification",
        indexes = {
                @Index(
                        name = "idx_phone_verification_phone_purpose_status_created",
                        columnList = "telephone_e164,purpose,status,created_at"
                ),
                @Index(name = "idx_phone_verification_otp_expires", columnList = "otp_expires_at"),
                @Index(name = "idx_phone_verification_token_expires", columnList = "verification_token_expires_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhoneVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    /** Numéro normalisé au format E.164, ex. +22890123456. */
    @Column(name = "telephone_e164", nullable = false, length = 20)
    private String telephoneE164;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 30)
    private PhoneVerificationPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PhoneVerificationStatus status;

    /** Hash SHA-256 hexadécimal de l'OTP. */
    @Column(name = "otp_hash", nullable = false, length = 64)
    private String otpHash;

    @Column(name = "otp_expires_at", nullable = false)
    private Instant otpExpiresAt;

    /** Nombre total de tentatives de saisie sur ce challenge. */
    @Column(name = "attempts", nullable = false)
    private int attempts;

    /** Nombre total de SMS envoyés pour ce challenge. */
    @Column(name = "send_count", nullable = false)
    private int sendCount;

    /** Dernier envoi SMS réussi. */
    @Column(name = "last_sent_at", nullable = false)
    private Instant lastSentAt;

    /** Date avant laquelle un nouvel envoi est interdit. */
    @Column(name = "next_send_allowed_at", nullable = false)
    private Instant nextSendAllowedAt;

    /** Renseigné uniquement après validation correcte de l'OTP. */
    @Column(name = "verified_at")
    private Instant verifiedAt;

    /**
     * Hash SHA-256 d'un jeton opaque remis au client après validation OTP.
     * Ce jeton constitue la preuve à présenter au flux métier puis est consommé une seule fois.
     */
    @Column(name = "verification_token_hash", unique = true, length = 64)
    private String verificationTokenHash;

    @Column(name = "verification_token_expires_at")
    private Instant verificationTokenExpiresAt;

    /** Horodatage de consommation de la preuve par le flux métier. */
    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "request_ip", length = 64)
    private String requestIp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static PhoneVerification create(
            String telephoneE164,
            PhoneVerificationPurpose purpose,
            String otpHash,
            Instant otpExpiresAt,
            Instant sentAt,
            Instant nextSendAllowedAt,
            String requestIp
    ) {
        PhoneVerification verification = new PhoneVerification();
        verification.telephoneE164 = requireText(telephoneE164, "telephoneE164");
        verification.purpose = Objects.requireNonNull(purpose, "purpose");
        verification.otpHash = requireText(otpHash, "otpHash");
        verification.otpExpiresAt = Objects.requireNonNull(otpExpiresAt, "otpExpiresAt");
        verification.lastSentAt = Objects.requireNonNull(sentAt, "sentAt");
        verification.nextSendAllowedAt = Objects.requireNonNull(nextSendAllowedAt, "nextSendAllowedAt");
        verification.requestIp = requestIp;
        verification.status = PhoneVerificationStatus.PENDING;
        verification.attempts = 0;
        verification.sendCount = 1;
        verification.createdAt = sentAt;
        verification.updatedAt = sentAt;
        return verification;
    }

    public boolean isOtpExpired(Instant now) {
        return otpExpiresAt == null || !otpExpiresAt.isAfter(now);
    }

    public boolean isVerificationTokenExpired(Instant now) {
        return verificationTokenExpiresAt == null || !verificationTokenExpiresAt.isAfter(now);
    }

    public boolean isPending() {
        return status == PhoneVerificationStatus.PENDING;
    }

    public boolean isVerified() {
        return status == PhoneVerificationStatus.VERIFIED;
    }

    public boolean isConsumed() {
        return status == PhoneVerificationStatus.CONSUMED;
    }

    public boolean canVerify(int maxAttempts, Instant now) {
        return isPending()
                && attempts < maxAttempts
                && !isOtpExpired(now);
    }

    public boolean canResend(int maxSendCount, Instant now) {
        return isPending()
                && sendCount < maxSendCount
                && !nextSendAllowedAt.isAfter(now)
                && !isOtpExpired(now);
    }

    public void registerFailedAttempt(int maxAttempts, Instant now) {
        requireStatus(PhoneVerificationStatus.PENDING);
        this.attempts++;
        if (this.attempts >= maxAttempts) {
            this.status = PhoneVerificationStatus.BLOCKED;
        }
        touch(now);
    }

    /**
     * Réémission dans le même challenge : nouvel OTP, mais le nombre de tentatives
     * n'est pas remis à zéro afin qu'un renvoi ne contourne pas la protection anti-bruteforce.
     */
    public void rotateOtp(
            String newOtpHash,
            Instant newOtpExpiresAt,
            Instant sentAt,
            Instant nextSendAllowedAt
    ) {
        requireStatus(PhoneVerificationStatus.PENDING);
        this.otpHash = requireText(newOtpHash, "newOtpHash");
        this.otpExpiresAt = Objects.requireNonNull(newOtpExpiresAt, "newOtpExpiresAt");
        this.lastSentAt = Objects.requireNonNull(sentAt, "sentAt");
        this.nextSendAllowedAt = Objects.requireNonNull(nextSendAllowedAt, "nextSendAllowedAt");
        this.sendCount++;
        touch(sentAt);
    }

    public void markVerified(
            String tokenHash,
            Instant tokenExpiresAt,
            Instant now
    ) {
        requireStatus(PhoneVerificationStatus.PENDING);
        this.status = PhoneVerificationStatus.VERIFIED;
        this.verifiedAt = Objects.requireNonNull(now, "now");
        this.verificationTokenHash = requireText(tokenHash, "tokenHash");
        this.verificationTokenExpiresAt = Objects.requireNonNull(tokenExpiresAt, "tokenExpiresAt");
        touch(now);
    }

    public void markConsumed(Instant now) {
        requireStatus(PhoneVerificationStatus.VERIFIED);
        this.status = PhoneVerificationStatus.CONSUMED;
        this.consumedAt = Objects.requireNonNull(now, "now");
        touch(now);
    }

    public void markExpired(Instant now) {
        if (status == PhoneVerificationStatus.CONSUMED
                || status == PhoneVerificationStatus.BLOCKED
                || status == PhoneVerificationStatus.EXPIRED) {
            return;
        }
        this.status = PhoneVerificationStatus.EXPIRED;
        touch(now);
    }

    public void markBlocked(Instant now) {
        if (status == PhoneVerificationStatus.CONSUMED
                || status == PhoneVerificationStatus.EXPIRED) {
            return;
        }
        this.status = PhoneVerificationStatus.BLOCKED;
        touch(now);
    }

    private void requireStatus(PhoneVerificationStatus expected) {
        if (status != expected) {
            throw new IllegalStateException(
                    "Transition PhoneVerification invalide : statut=" + status + ", attendu=" + expected
            );
        }
    }

    private void touch(Instant now) {
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " est obligatoire");
        }
        return value;
    }
}
