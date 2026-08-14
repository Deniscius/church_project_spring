package com.eyram.dev.church_project_spring.enums;

/**
 * Finalité métier d'une vérification de numéro de téléphone.
 * Une preuve ne peut être consommée que pour le flux pour lequel elle a été émise.
 */
public enum PhoneVerificationPurpose {
    DEMANDE_CREATION,
    SUIVI_DEMANDE
}
