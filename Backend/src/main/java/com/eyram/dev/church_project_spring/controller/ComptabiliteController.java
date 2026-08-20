package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.DemandeReversementRequest;
import com.eyram.dev.church_project_spring.DTO.request.ReversementDecisionRequest;
import com.eyram.dev.church_project_spring.DTO.response.AbonnementResponse;
import com.eyram.dev.church_project_spring.DTO.response.CompteParoisseResponse;
import com.eyram.dev.church_project_spring.DTO.response.DemandeReversementResponse;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseResponse;
import com.eyram.dev.church_project_spring.mappers.ParoisseMapper;
import com.eyram.dev.church_project_spring.service.accounting.ReversementService;
import com.eyram.dev.church_project_spring.service.billing.SubscriptionBillingService;
import com.eyram.dev.church_project_spring.service.tenant.TenantCatalogBootstrapService;
import com.eyram.dev.church_project_spring.utils.SecurityUtils;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Comptabilité avec séparation des pouvoirs : consultation, demande locale,
 * reversement et administration d'abonnement sont des permissions distinctes.
 */
@RestController
@RequestMapping("/comptabilite")
@RequiredArgsConstructor
public class ComptabiliteController {

    private final ReversementService reversementService;
    private final SubscriptionBillingService subscriptionBillingService;
    private final TenantCatalogBootstrapService tenantCatalogBootstrapService;
    private final ParoisseMapper paroisseMapper;

    @GetMapping("/comptes/{paroissePublicId}")
    @PreAuthorize("hasAnyAuthority('treasury:read', 'finance:read')")
    public ResponseEntity<CompteParoisseResponse> compte(@PathVariable UUID paroissePublicId) {
        return ResponseEntity.ok(reversementService.getCompte(paroissePublicId));
    }

    @GetMapping("/reversements")
    @PreAuthorize("hasAuthority('finance:read')")
    public ResponseEntity<List<DemandeReversementResponse>> listReversements() {
        return ResponseEntity.ok(reversementService.listAll());
    }

    @GetMapping("/reversements/paroisse/{paroissePublicId}")
    @PreAuthorize("hasAnyAuthority('treasury:read', 'finance:read')")
    public ResponseEntity<List<DemandeReversementResponse>> listByParoisse(@PathVariable UUID paroissePublicId) {
        return ResponseEntity.ok(reversementService.listByParoisse(paroissePublicId));
    }

    @PostMapping("/reversements")
    @PreAuthorize("hasAuthority('treasury:manage')")
    public ResponseEntity<DemandeReversementResponse> demander(@Valid @RequestBody DemandeReversementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reversementService.demander(request));
    }

    @PostMapping("/reversements/{publicId}/payer")
    @PreAuthorize("hasAuthority('payout:manage')")
    public ResponseEntity<DemandeReversementResponse> payer(
            @PathVariable UUID publicId,
            @Valid @RequestBody ReversementDecisionRequest request
    ) {
        String traitePar = String.valueOf(SecurityUtils.getCurrentUserPublicId());
        return ResponseEntity.ok(reversementService.marquerPaye(publicId, request, traitePar));
    }

    @PostMapping("/reversements/{publicId}/rejeter")
    @PreAuthorize("hasAuthority('payout:manage')")
    public ResponseEntity<DemandeReversementResponse> rejeter(
            @PathVariable UUID publicId,
            @RequestBody(required = false) ReversementDecisionRequest request
    ) {
        String traitePar = String.valueOf(SecurityUtils.getCurrentUserPublicId());
        return ResponseEntity.ok(reversementService.rejeter(
                publicId,
                request != null ? request : new ReversementDecisionRequest(null, null),
                traitePar
        ));
    }

    @GetMapping("/abonnements")
    @PreAuthorize("hasAuthority('subscription:read')")
    public ResponseEntity<List<AbonnementResponse>> listAbonnements() {
        return ResponseEntity.ok(subscriptionBillingService.listAbonnements());
    }

    @GetMapping("/catalogue-modele")
    @PreAuthorize("hasAuthority('schedule:manage') and principal.isGlobal()")
    public ResponseEntity<ParoisseResponse> catalogueModele() {
        return ResponseEntity.ok(
                tenantCatalogBootstrapService.findTemplateParoisse()
                        .map(paroisseMapper::modelToDto)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Catalogue plateforme introuvable"
                        ))
        );
    }

    @PostMapping("/abonnements/checkout/{paroissePublicId}")
    @PreAuthorize("hasAuthority('subscription:checkout')")
    public ResponseEntity<Map<String, Object>> checkoutAbonnement(
            @PathVariable UUID paroissePublicId,
            @RequestParam(required = false) String plan
    ) {
        return ResponseEntity.ok(subscriptionBillingService.checkout(paroissePublicId, plan));
    }

    @PostMapping("/abonnements/{paroissePublicId}/activer")
    @PreAuthorize("hasAuthority('subscription:activate')")
    public ResponseEntity<Map<String, Object>> activerAbonnement(
            @PathVariable UUID paroissePublicId,
            @RequestParam(required = false) String plan
    ) {
        return ResponseEntity.ok(subscriptionBillingService.activateManually(paroissePublicId, plan));
    }

    @PostMapping("/abonnements/{paroissePublicId}/prolonger")
    @PreAuthorize("hasAuthority('subscription:manage')")
    public ResponseEntity<Map<String, Object>> prolongerAbonnement(
            @PathVariable UUID paroissePublicId,
            @RequestParam(defaultValue = "30") int jours
    ) {
        return ResponseEntity.ok(subscriptionBillingService.prolonger(paroissePublicId, jours));
    }

    @PostMapping("/abonnements/{abonnementPublicId}/annuler")
    @PreAuthorize("hasAuthority('subscription:manage')")
    public ResponseEntity<Map<String, Object>> annulerAbonnementPending(
            @PathVariable UUID abonnementPublicId
    ) {
        return ResponseEntity.ok(subscriptionBillingService.annulerPending(abonnementPublicId));
    }

    @PostMapping("/abonnements/{paroissePublicId}/resilier")
    @PreAuthorize("hasAuthority('subscription:manage')")
    public ResponseEntity<Map<String, Object>> resilierAbonnement(
            @PathVariable UUID paroissePublicId
    ) {
        return ResponseEntity.ok(subscriptionBillingService.resilier(paroissePublicId));
    }
}
