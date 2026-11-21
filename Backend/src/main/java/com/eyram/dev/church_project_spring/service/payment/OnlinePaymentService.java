package com.eyram.dev.church_project_spring.service.payment;

import com.eyram.dev.church_project_spring.DTO.response.PaymentCheckoutResponse;
import com.eyram.dev.church_project_spring.DTO.response.PaymentFeeBreakdown;
import com.eyram.dev.church_project_spring.service.payment.saspay.SasPayPaymentService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** SasPay is the only online provider; paid historical rows remain readable. */
@Service
@RequiredArgsConstructor
public class OnlinePaymentService {
  private final SasPayPaymentService saspay;
  private final PaymentReturnTokenService tokens;

  public PaymentFeeBreakdown quoteByTrackingCode(String code) {
    return saspay.quote(code);
  }

  public PaymentCheckoutResponse checkout(String code, String email) {
    return saspay.checkout(code, email);
  }

  public PaymentCheckoutResponse reconcileByTrackingCode(String code, String ignoredHint) {
    return saspay.reconcile(code);
  }

  public Map<String, String> resolveReturnAndReconcile(String token, String ignoredHint) {
    String code = tokens.resolve(token);
    var status = saspay.reconcile(code);
    return Map.of("codeSuivie", code, "statutPaiement", status.statutPaiement().name());
  }
}
