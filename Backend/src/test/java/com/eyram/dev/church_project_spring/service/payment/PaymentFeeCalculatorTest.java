package com.eyram.dev.church_project_spring.service.payment;

import com.eyram.dev.church_project_spring.config.FedaPayProperties;
import com.eyram.dev.church_project_spring.config.PlatformBillingProperties;
import com.eyram.dev.church_project_spring.enums.FeePayer;
import com.eyram.dev.church_project_spring.enums.ModePaiement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentFeeCalculatorTest {

    private PaymentFeeCalculator calculator;
    private FedaPayProperties fedaPayProperties;
    private PlatformBillingProperties platformProperties;

    @BeforeEach
    void setUp() {
        fedaPayProperties = new FedaPayProperties();
        fedaPayProperties.getFees().setPayer(FeePayer.CUSTOMER);
        fedaPayProperties.getFees().setTmoneyPercent(new BigDecimal("1.80"));
        fedaPayProperties.getFees().setFloozPercent(new BigDecimal("1.80"));
        fedaPayProperties.getFees().setCartePercent(new BigDecimal("3.60"));
        fedaPayProperties.getFees().setFixedXof(0);

        platformProperties = new PlatformBillingProperties();
        platformProperties.setCommissionPercent(new BigDecimal("5.00"));
        platformProperties.setCommissionFixedXof(0);

        calculator = new PaymentFeeCalculator(fedaPayProperties, platformProperties);
    }

    @Test
    void customerPaysPlatformAndAggregatorFees() {
        // P=10000 ; plateforme 5%=500 ; agrégateur 1.8% de 10500 = 189 ; charge = 10689
        var fees = calculator.calculate(10_000, ModePaiement.TMONEY);
        assertEquals(500, fees.montantFraisPlateforme());
        assertEquals(189, fees.montantFraisAgregeateur());
        assertEquals(689, fees.montantFrais());
        assertEquals(10_689, fees.montantCharge());
        assertEquals(10_000, fees.montantNetParoisse());
        assertEquals(500, fees.montantNetPlateforme());
    }

    @Test
    void cashHasZeroFees() {
        var fees = calculator.calculate(5_000, ModePaiement.ESPECES);
        assertEquals(0, fees.montantFrais());
        assertEquals(5_000, fees.montantCharge());
        assertEquals(5_000, fees.montantNetParoisse());
    }
}
