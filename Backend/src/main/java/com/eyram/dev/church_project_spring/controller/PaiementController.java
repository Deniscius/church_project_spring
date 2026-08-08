package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.response.PaymentCheckoutResponse;
import com.eyram.dev.church_project_spring.DTO.response.PaymentFeeBreakdown;
import com.eyram.dev.church_project_spring.service.payment.FedaPayPaymentService;
import com.eyram.dev.church_project_spring.service.payment.PaymentReturnTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/paiements")
@RequiredArgsConstructor
public class PaiementController {

    private final FedaPayPaymentService fedaPayPaymentService;
    private final PaymentReturnTokenService paymentReturnTokenService;

    @GetMapping("/quote/{codeSuivie}")
    public ResponseEntity<PaymentFeeBreakdown> quote(@PathVariable String codeSuivie) {
        return ResponseEntity.ok(fedaPayPaymentService.quoteByTrackingCode(codeSuivie));
    }

    @PostMapping("/checkout/{codeSuivie}")
    public ResponseEntity<PaymentCheckoutResponse> checkout(@PathVariable String codeSuivie) {
        log.info("POST /paiements/checkout — préparation FedaPay");
        return ResponseEntity.ok(fedaPayPaymentService.checkout(codeSuivie));
    }

    /**
     * Résout un jeton opaque de retour FedaPay vers le code de suivi (jamais exposé dans l'URL longue durée).
     */
    @PostMapping("/retour/resoudre")
    public ResponseEntity<Map<String, String>> resolveReturn(@RequestBody Map<String, String> body) {
        String token = body == null ? null : body.get("token");
        String code = paymentReturnTokenService.resolve(token);
        return ResponseEntity.ok(Map.of("codeSuivie", code));
    }
}
