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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        log.info("POST /paiements/checkout/{} - FedaPay checkout", codeSuivie);
        return ResponseEntity.ok(fedaPayPaymentService.checkout(codeSuivie));
    }
}
