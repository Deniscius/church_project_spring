package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.DemandeReversementRequest;
import com.eyram.dev.church_project_spring.DTO.request.ReversementDecisionRequest;
import com.eyram.dev.church_project_spring.DTO.response.AbonnementResponse;
import com.eyram.dev.church_project_spring.DTO.response.CompteParoisseResponse;
import com.eyram.dev.church_project_spring.DTO.response.DemandeReversementResponse;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseResponse;
import com.eyram.dev.church_project_spring.enums.PlanAbonnement;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Séparation des pouvoirs : le SUPER_ADMIN constitue l'équipe plateforme et
 * consulte les finances, mais seul le COMPTABLE exécute les mouvements
 * d'argent (paiement/rejet de reversement, activation d'abonnement).
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
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPTABLE_LOCAL', 'COMPTABLE', 'SUPER_ADMIN')")
    public ResponseEntity<CompteParoisseResponse> compte(@PathVariable UUID paroissePublicId) {
        return ResponseEntity.ok(reversementService.getCompte(paroissePublicId));
    }

    @GetMapping("/reversements")
    @PreAuthorize("hasAnyRole('COMPTABLE', 'SUPER_ADMIN')")
    public ResponseEntity<List<DemandeReversementResponse>> listReversements() {
        return ResponseEntity.ok(reversementService.listAll());
    }

    @GetMapping("/reversements/paroisse/{paroissePublicId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPTABLE_LOCAL', 'COMPTABLE', 'SUPER_ADMIN')")
    public ResponseEntity<List<DemandeReversementResponse>> listByParoisse(@PathVariable UUID paroissePublicId) {
        return ResponseEntity.ok(reversementService.listByParoisse(paroissePublicId));
    }

    @PostMapping("/reversements")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DemandeReversementResponse> demander(@Valid @RequestBody DemandeReversementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reversementService.demander(request));
    }

    @PostMapping("/reversements/{publicId}/payer")
    @PreAuthorize("hasRole('COMPTABLE')")
    public ResponseEntity<DemandeReversementResponse> payer(
            @PathVariable UUID publicId,
            @Valid @RequestBody ReversementDecisionRequest request
    ) {
        String traitePar = String.valueOf(SecurityUtils.getCurrentUserPublicId());
        return ResponseEntity.ok(reversementService.marquerPaye(publicId, request, traitePar));
    }

    @PostMapping("/reversements/{publicId}/rejeter")
    @PreAuthorize("hasRole('COMPTABLE')")
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
    @PreAuthorize("hasAnyRole('COMPTABLE', 'SUPER_ADMIN')")
    public ResponseEntity<List<AbonnementResponse>> listAbonnements() {
        return ResponseEntity.ok(subscriptionBillingService.listAbonnements());
    }

    /**
     * Support technique du catalogue plateforme (horaires, types, forfaits)
     * cloné vers chaque nouveau tenant. Indépendant de toute paroisse cliente.
     */
    @GetMapping("/catalogue-modele")
    @PreAuthorize("hasAnyRole('COMPTABLE', 'SUPER_ADMIN')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPTABLE')")
    public ResponseEntity<Map<String, Object>> checkoutAbonnement(
            @PathVariable UUID paroissePublicId,
            @RequestParam(required = false) PlanAbonnement plan
    ) {
        return ResponseEntity.ok(subscriptionBillingService.checkout(paroissePublicId, plan));
    }

    /**
     * Confirme le paiement d'abonnement (hors ligne / FedaPay déjà reçu) et active la paroisse.
     * L'admin local peut ensuite se connecter. Accessible au SUPER_ADMIN (onboarding
     * depuis l'annuaire) et au COMPTABLE (circuit financier).
     */
    @PostMapping("/abonnements/{paroissePublicId}/activer")
    @PreAuthorize("hasAnyRole('COMPTABLE', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> activerAbonnement(
            @PathVariable UUID paroissePublicId,
            @RequestParam(required = false) PlanAbonnement plan
    ) {
        return ResponseEntity.ok(subscriptionBillingService.activateManually(paroissePublicId, plan));
    }

    /** Prolongation gracieuse (jours) sans nouveau cycle de facturation. */
    @PostMapping("/abonnements/{paroissePublicId}/prolonger")
    @PreAuthorize("hasRole('COMPTABLE')")
    public ResponseEntity<Map<String, Object>> prolongerAbonnement(
            @PathVariable UUID paroissePublicId,
            @RequestParam(defaultValue = "30") int jours
    ) {
        return ResponseEntity.ok(subscriptionBillingService.prolonger(paroissePublicId, jours));
    }

    /** Annule un lien de paiement d'abonnement abandonné. */
    @PostMapping("/abonnements/{abonnementPublicId}/annuler")
    @PreAuthorize("hasRole('COMPTABLE')")
    public ResponseEntity<Map<String, Object>> annulerAbonnementPending(
            @PathVariable UUID abonnementPublicId
    ) {
        return ResponseEntity.ok(subscriptionBillingService.annulerPending(abonnementPublicId));
    }

    /** Résilie l'accès SaaS de la paroisse. */
    @PostMapping("/abonnements/{paroissePublicId}/resilier")
    @PreAuthorize("hasRole('COMPTABLE')")
    public ResponseEntity<Map<String, Object>> resilierAbonnement(
            @PathVariable UUID paroissePublicId
    ) {
        return ResponseEntity.ok(subscriptionBillingService.resilier(paroissePublicId));
    }
}
