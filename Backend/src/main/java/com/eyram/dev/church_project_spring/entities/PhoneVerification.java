package com.eyram.dev.church_project_spring.entities;

import com.eyram.dev.church_project_spring.enums.PhoneVerificationPurpose;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
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
                        name = "idx_phone_verification_phone_purpose_created",
                        columnList = "telephone_e164,purpose,created_at"
                ),
                @Index(name = "idx_phone_verification_otp_expires", columnList = "otp_expires_at"),
                @Index(name = "idx_phone_verification_token_expires", columnList = "verification_token_expires_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
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

    /** Hash SHA-256 hexadécimal de l'OTP. */
    @Column(name = "otp_hash", nullable = false, length = 64)
    private String otpHash;

    @Column(name = "otp_expires_at", nullable = false)
    private LocalDateTime otpExpiresAt;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    /** Date du dernier envoi SMS, utilisée pour le cooldown de renvoi. */
    @Column(name = "last_sent_at", nullable = false)
    private LocalDateTime lastSentAt = LocalDateTime.now();

    /** Renseigné uniquement après validation correcte de l'OTP. */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    /**
     * Hash SHA-256 d'un jeton opaque remis au client après validation OTP.
     * Ce jeton constitue la preuve à présenter au flux métier puis est consommé une seule fois.
     */
    @Column(name = "verification_token_hash", unique = true, length = 64)
    private String verificationTokenHash;

    @Column(name = "verification_token_expires_at")
    private LocalDateTime verificationTokenExpiresAt;

    /** Horodatage de consommation de la preuve par le flux métier. */
    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "request_ip", length = 64)
    private String requestIp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public boolean isOtpExpired(LocalDateTime now) {
        return otpExpiresAt == null || !otpExpiresAt.isAfter(now);
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public boolean isVerificationTokenExpired(LocalDateTime now) {
        return verificationTokenExpiresAt == null || !verificationTokenExpiresAt.isAfter(now);
    }

    public boolean canVerify(int maxAttempts, LocalDateTime now) {
        return !isVerified()
                && !isConsumed()
                && attempts < maxAttempts
                && !isOtpExpired(now);
    }

    public void registerFailedAttempt() {
        attempts++;
        touch();
    }

    public void refreshOtp(String newOtpHash, LocalDateTime newOtpExpiresAt, LocalDateTime sentAt) {
        this.otpHash = newOtpHash;
        this.otpExpiresAt = newOtpExpiresAt;
        this.lastSentAt = sentAt;
        this.attempts = 0;
        this.verifiedAt = null;
        this.verificationTokenHash = null;
        this.verificationTokenExpiresAt = null;
        this.consumedAt = null;
        touch();
    }

    public void markVerified(
            LocalDateTime verifiedAt,
            String tokenHash,
            LocalDateTime tokenExpiresAt
    ) {
        this.verifiedAt = verifiedAt;
        this.verificationTokenHash = tokenHash;
        this.verificationTokenExpiresAt = tokenExpiresAt;
        touch();
    }

    public void markConsumed(LocalDateTime consumedAt) {
        this.consumedAt = consumedAt;
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
