package com.eyram.dev.church_project_spring.enums;

/**
 * Qui supporte les frais de l'agrégateur.
 * MERCHANT : le fidèle paie le montant facture ; la paroisse reçoit montant − frais.
 * CUSTOMER : le fidèle paie montant + frais ; la paroisse reçoit le montant facture.
 */
public enum FeePayer {
    MERCHANT,
    CUSTOMER
}
