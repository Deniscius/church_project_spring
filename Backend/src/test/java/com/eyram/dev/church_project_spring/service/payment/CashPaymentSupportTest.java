package com.eyram.dev.church_project_spring.service.payment;

import static org.assertj.core.api.Assertions.*;

import com.eyram.dev.church_project_spring.config.FedaPayProperties;
import com.eyram.dev.church_project_spring.config.PlatformBillingProperties;
import com.eyram.dev.church_project_spring.entities.DetailsPaiement;
import com.eyram.dev.church_project_spring.enums.ModePaiement;
import org.junit.jupiter.api.Test;

class CashPaymentSupportTest {
    private final CashPaymentSupport cash = new CashPaymentSupport();

    @Test
    void cashSnapshotKeepsThePreviousAmountsAndPreservesLocalProvider() {
        var old = new PaymentFeeCalculator(new FedaPayProperties(), new PlatformBillingProperties());
        for (int amount : new int[] {0, 1, 5000, 100_000}) {
            var expected = old.calculate(amount, ModePaiement.ESPECES);
            var details = new DetailsPaiement();
            details.setProvider(CashPaymentSupport.PROVIDER_CAISSE_LOCALE);
            details.setMontantFrais(999);
            cash.applyFeeSnapshot(details, ModePaiement.ESPECES, amount);
            assertThat(details.getMontant()).isEqualTo(expected.montantFacture());
            assertThat(details.getMontantCharge()).isEqualTo(expected.montantCharge());
            assertThat(details.getMontantNet()).isEqualTo(expected.montantNetParoisse());
            assertThat(details.getMontantFrais()).isEqualTo(expected.montantFrais());
            assertThat(details.getMontantFraisAgregeateur()).isZero();
            assertThat(details.getMontantFraisPlateforme()).isZero();
            assertThat(details.getProvider()).isEqualTo("CAISSE_LOCALE");
        }
    }

    @Test
    void manualProviderFallbackAndValidationRemainLocal() {
        var details = new DetailsPaiement();
        cash.applyFeeSnapshot(details, null, 5000);
        assertThat(details.getProvider()).isEqualTo("MANUEL");
        assertThatThrownBy(() -> cash.applyFeeSnapshot(details, ModePaiement.ESPECES, -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cash.applyFeeSnapshot(details, ModePaiement.TMONEY, 5000))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
