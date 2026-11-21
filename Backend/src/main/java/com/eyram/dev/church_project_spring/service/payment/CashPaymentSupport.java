package com.eyram.dev.church_project_spring.service.payment;

import com.eyram.dev.church_project_spring.entities.DetailsPaiement;
import com.eyram.dev.church_project_spring.enums.ModePaiement;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Cash snapshots are local, fee-free and independent of any online provider. */
@Component
public class CashPaymentSupport {
  public static final String PROVIDER_CAISSE_LOCALE = "CAISSE_LOCALE";

  public void applyFeeSnapshot(DetailsPaiement details, ModePaiement mode, int montantFacture) {
    if (mode != null && mode != ModePaiement.ESPECES) {
      throw new IllegalArgumentException("Ce snapshot est réservé aux espèces");
    }
    if (montantFacture < 0) throw new IllegalArgumentException("Montant facture invalide");
    details.setMontant(montantFacture);
    details.setMontantFrais(0);
    details.setMontantFraisAgregeateur(0);
    details.setMontantFraisPlateforme(0);
    details.setMontantCharge(montantFacture);
    details.setMontantNet(montantFacture);
    // Preserve the historical manual/cash labels and any existing historical provider.
    if (!StringUtils.hasText(details.getProvider())) details.setProvider("MANUEL");
  }
}
