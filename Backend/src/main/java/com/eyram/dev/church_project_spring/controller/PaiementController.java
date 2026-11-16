package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.response.PaymentCheckoutResponse;
import com.eyram.dev.church_project_spring.DTO.response.PaymentFeeBreakdown;
import com.eyram.dev.church_project_spring.service.payment.OnlinePaymentService;
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

    private final OnlinePaymentService payments;

    @GetMapping("/quote/{codeSuivie}")
    public ResponseEntity<PaymentFeeBreakdown> quote(@PathVariable String codeSuivie) {
        return ResponseEntity.ok(payments.quoteByTrackingCode(codeSuivie));
    }

    public record CheckoutInput(@jakarta.validation.constraints.Email @jakarta.validation.constraints.Size(max=254) String email){}

    @PostMapping("/checkout/{codeSuivie}")
    public ResponseEntity<PaymentCheckoutResponse> checkout(@PathVariable String codeSuivie, @jakarta.validation.Valid @RequestBody(required=false) CheckoutInput input) {
        log.info("POST /paiements/checkout — préparation SasPay");
        return ResponseEntity.ok(payments.checkout(codeSuivie,input==null?null:input.email()));
    }

    /**
     * Retour navigateur du prestataire ({@code callback_url}) :
     * résout le jeton opaque puis réconcilie le statut via l'API du prestataire (GET de vérification).
     * Le query param {@code status} n'est pas une source de vérité.
     */
    @PostMapping("/retour/resoudre")
    public ResponseEntity<Map<String, String>> resolveReturn(@RequestBody Map<String, String> body) {
        String token = body == null ? null : body.get("token");
        String providerTxId = body == null ? null : body.get("providerTransactionId");
        return ResponseEntity.ok(payments.resolveReturnAndReconcile(token, providerTxId));
    }

    /** Synchronise le statut local avec le prestataire enregistré pour un code de suivi. */
    @PostMapping("/reconcile/{codeSuivie}")
    public ResponseEntity<PaymentCheckoutResponse> reconcile(@PathVariable String codeSuivie) {
        return ResponseEntity.ok(payments.reconcileByTrackingCode(codeSuivie, null));
    }
}
