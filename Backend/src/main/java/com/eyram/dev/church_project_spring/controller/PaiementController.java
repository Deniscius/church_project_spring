package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.response.PaymentCheckoutResponse;
import com.eyram.dev.church_project_spring.DTO.response.PaymentFeeBreakdown;
import com.eyram.dev.church_project_spring.service.payment.FedaPayPaymentService;
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
     * Retour navigateur FedaPay ({@code callback_url}) :
     * résout le jeton opaque puis réconcilie le statut via l'API FedaPay (GET transaction).
     * Le query param {@code status} n'est pas une source de vérité.
     */
    @PostMapping("/retour/resoudre")
    public ResponseEntity<Map<String, String>> resolveReturn(@RequestBody Map<String, String> body) {
        String token = body == null ? null : body.get("token");
        String providerTxId = body == null ? null : body.get("providerTransactionId");
        return ResponseEntity.ok(fedaPayPaymentService.resolveReturnAndReconcile(token, providerTxId));
    }

    /** Synchronise le statut local avec FedaPay pour un code de suivi. */
    @PostMapping("/reconcile/{codeSuivie}")
    public ResponseEntity<PaymentCheckoutResponse> reconcile(@PathVariable String codeSuivie) {
        return ResponseEntity.ok(fedaPayPaymentService.reconcileByTrackingCode(codeSuivie, null));
    }
}
