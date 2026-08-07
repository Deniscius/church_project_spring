package com.eyram.dev.church_project_spring.service.payment;

import com.eyram.dev.church_project_spring.DTO.response.PaymentFeeBreakdown;
import com.eyram.dev.church_project_spring.config.FedaPayProperties;
import com.eyram.dev.church_project_spring.config.PlatformBillingProperties;
import com.eyram.dev.church_project_spring.enums.FeePayer;
import com.eyram.dev.church_project_spring.enums.ModePaiement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Fidèle paie les frais supplémentaires (agrégateur + plateforme).
 * La paroisse conserve le prix de la messe ; la plateforme et FedaPay prennent leur part sur le surplus.
 */
@Component
@RequiredArgsConstructor
public class PaymentFeeCalculator {

    private final FedaPayProperties fedaPayProperties;
    private final PlatformBillingProperties platformProperties;

    public PaymentFeeBreakdown calculate(int montantFacture, ModePaiement mode) {
        if (montantFacture < 0) {
            throw new IllegalArgumentException("Montant facture invalide");
        }

        FeePayer payer = fedaPayProperties.getFees().getPayer() != null
                ? fedaPayProperties.getFees().getPayer()
                : FeePayer.CUSTOMER;

        if (mode == null || mode == ModePaiement.ESPECES) {
            return zeroExtras(mode == null ? ModePaiement.ESPECES : mode, payer, montantFacture);
        }

        BigDecimal aggPercent = percentFor(mode);
        int aggFixed = Math.max(0, fedaPayProperties.getFees().getFixedXof());
        BigDecimal platPercent = nullToZero(platformProperties.getCommissionPercent());
        int platFixed = Math.max(0, platformProperties.getCommissionFixedXof());

        int fraisPlateforme = percentOf(montantFacture, platPercent) + platFixed;
        int basePourAgregeateur = montantFacture + fraisPlateforme;
        int fraisAgregeateur = percentOf(basePourAgregeateur, aggPercent) + aggFixed;
        int fraisTotal = fraisPlateforme + fraisAgregeateur;

        if (payer == FeePayer.MERCHANT) {
            // Mode legacy : fidèle paie la facture ; net paroisse réduit (déconseillé).
            int netParoisse = Math.max(0, montantFacture - fraisTotal);
            return new PaymentFeeBreakdown(
                    mode, payer, aggPercent, platPercent, aggFixed, platFixed,
                    montantFacture, fraisAgregeateur, fraisPlateforme, fraisTotal,
                    montantFacture, netParoisse, 0
            );
        }

        // CUSTOMER (modèle cible) : fidèle paie facture + extras ; paroisse = prix messe ; plateforme = sa commission.
        return new PaymentFeeBreakdown(
                mode, payer, aggPercent, platPercent, aggFixed, platFixed,
                montantFacture, fraisAgregeateur, fraisPlateforme, fraisTotal,
                montantFacture + fraisTotal, montantFacture, fraisPlateforme
        );
    }

    private PaymentFeeBreakdown zeroExtras(ModePaiement mode, FeePayer payer, int montantFacture) {
        return new PaymentFeeBreakdown(
                mode, payer, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0,
                montantFacture, 0, 0, 0,
                montantFacture, montantFacture, 0
        );
    }

    private BigDecimal percentFor(ModePaiement mode) {
        FedaPayProperties.Fees fees = fedaPayProperties.getFees();
        return switch (mode) {
            case TMONEY -> nullToZero(fees.getTmoneyPercent());
            case FLOOZ -> nullToZero(fees.getFloozPercent());
            case CARTE -> nullToZero(fees.getCartePercent());
            case ESPECES -> BigDecimal.ZERO;
        };
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static int percentOf(int base, BigDecimal percent) {
        if (percent == null || percent.signum() <= 0 || base <= 0) {
            return 0;
        }
        return BigDecimal.valueOf(base)
                .multiply(percent)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .intValueExact();
    }
}
