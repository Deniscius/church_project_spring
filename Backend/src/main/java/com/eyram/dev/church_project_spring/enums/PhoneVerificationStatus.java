package com.eyram.dev.church_project_spring.enums;

/**
 * Cycle de vie d'un challenge de vérification téléphonique.
 */
public enum PhoneVerificationStatus {
    PENDING,
    VERIFIED,
    CONSUMED,
    EXPIRED,
    BLOCKED
}
