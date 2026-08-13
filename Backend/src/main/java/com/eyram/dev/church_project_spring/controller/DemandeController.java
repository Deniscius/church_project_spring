package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.DemandeIntentionRequest;
import com.eyram.dev.church_project_spring.DTO.request.DemandeRequest;
import com.eyram.dev.church_project_spring.DTO.request.DemandeTypePaiementRequest;
import com.eyram.dev.church_project_spring.DTO.request.DemandeValidationRequest;
import com.eyram.dev.church_project_spring.DTO.request.TrackingByPhoneRequest;
import com.eyram.dev.church_project_spring.DTO.request.TrackingByPhoneVerifyRequest;
import com.eyram.dev.church_project_spring.DTO.response.DemandeParoisseStatsResponse;
import com.eyram.dev.church_project_spring.DTO.response.DemandePublicResponse;
import com.eyram.dev.church_project_spring.DTO.response.DemandeResponse;
import com.eyram.dev.church_project_spring.DTO.response.PageResponse;
import com.eyram.dev.church_project_spring.DTO.response.TrackingByPhoneChallengeResponse;
import com.eyram.dev.church_project_spring.DTO.response.TrackingByPhoneResponse;
import com.eyram.dev.church_project_spring.enums.StatutDemandeEnum;
import com.eyram.dev.church_project_spring.service.DemandeReceiptService;
import com.eyram.dev.church_project_spring.service.DemandeService;
import com.eyram.dev.church_project_spring.service.PublicDemandeViewService;
import com.eyram.dev.church_project_spring.service.TrackingPhoneOtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/demandes")
@RequiredArgsConstructor
public class DemandeController {

    private final DemandeService demandeService;
    private final DemandeReceiptService demandeReceiptService;
    private final PublicDemandeViewService publicDemandeViewService;
    private final TrackingPhoneOtpService trackingPhoneOtpService;

    @PostMapping
    public ResponseEntity<DemandeResponse> create(@Valid @RequestBody DemandeRequest request) {
        return new ResponseEntity<>(demandeService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{publicId}")
    public ResponseEntity<DemandeResponse> update(@PathVariable UUID publicId,
                                                  @Valid @RequestBody DemandeRequest request) {
        return ResponseEntity.ok(demandeService.update(publicId, request));
    }

    @PatchMapping("/{publicId}/validation")
    public ResponseEntity<DemandeResponse> updateValidation(
            @PathVariable UUID publicId,
            @Valid @RequestBody DemandeValidationRequest request
    ) {
        return ResponseEntity.ok(demandeService.updateValidation(publicId, request));
    }

    @PatchMapping("/{publicId}/intention")
    public ResponseEntity<DemandeResponse> updateIntention(
            @PathVariable UUID publicId,
            @Valid @RequestBody DemandeIntentionRequest request
    ) {
        return ResponseEntity.ok(demandeService.updateIntention(publicId, request));
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<DemandeResponse> getByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(demandeService.getByPublicId(publicId));
    }

    @GetMapping("/code/{codeSuivie}")
    public ResponseEntity<DemandePublicResponse> getByCodeSuivie(@PathVariable String codeSuivie) {
        return ResponseEntity.ok(publicDemandeViewService.toPublic(demandeService.getByCodeSuivie(codeSuivie)));
    }

    /**
     * Étape 1 : envoie un OTP à l'e-mail lié au téléphone (ne révèle pas les codes).
     */
    @PostMapping("/suivi/par-telephone")
    public ResponseEntity<TrackingByPhoneChallengeResponse> lookupByPhone(
            @Valid @RequestBody TrackingByPhoneRequest request
    ) {
        return ResponseEntity.ok(trackingPhoneOtpService.requestOtp(request.telephone()));
    }

    /** Étape 2 : après OTP, renvoie les codes de suivi. */
    @PostMapping("/suivi/par-telephone/verifier")
    public ResponseEntity<TrackingByPhoneResponse> verifyPhoneLookup(
            @Valid @RequestBody TrackingByPhoneVerifyRequest request
    ) {
        return ResponseEntity.ok(trackingPhoneOtpService.verifyOtp(request.telephone(), request.code()));
    }

    /** Public : le fidèle peut changer de mode tant que la demande n'est pas payée. */
    @PatchMapping("/code/{codeSuivie}/type-paiement")
    public ResponseEntity<DemandePublicResponse> updateTypePaiementByCode(
            @PathVariable String codeSuivie,
            @Valid @RequestBody DemandeTypePaiementRequest request
    ) {
        return ResponseEntity.ok(publicDemandeViewService.toPublic(
                demandeService.updateTypePaiementByCodeSuivie(codeSuivie, request.typePaiementPublicId())
        ));
    }

    /**
     * {@code /recu} sans extension : fetch JS (évite l’interception navigateur des URL {@code .pdf}).
     * {@code /recu.pdf} : ouverture directe / téléchargement.
     */
    @GetMapping(value = { "/code/{codeSuivie}/recu", "/code/{codeSuivie}/recu.pdf" },
            produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable String codeSuivie) {
        byte[] pdf = demandeReceiptService.generate(codeSuivie);
        return ResponseEntity.ok()
                // inline : aperçu navigateur / iframe ; le front gère le téléchargement en blob
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"recu-" + codeSuivie + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COMPTABLE', 'SUPER_ADMIN')")
    public ResponseEntity<PageResponse<DemandeResponse>> getAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "true") boolean includeDeleted
    ) {
        return ResponseEntity.ok(demandeService.getAllPaged(page, size, includeDeleted));
    }

    @GetMapping("/paroisse/{paroissePublicId}/stats")
    public ResponseEntity<DemandeParoisseStatsResponse> getParoisseStats(
            @PathVariable UUID paroissePublicId
    ) {
        return ResponseEntity.ok(demandeService.getParoisseStats(paroissePublicId));
    }

    /**
     * Liste paroisse toujours paginée (évite les dumps mémoire sous charge).
     * Sans {@code page}, renvoie la page 0 (taille 20).
     * {@code includeDeleted=true} : actives + archivées (comptable / admin uniquement).
     */
    @GetMapping("/paroisse/{paroissePublicId}")
    public ResponseEntity<PageResponse<DemandeResponse>> getByParoisse(
            @PathVariable UUID paroissePublicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeDeleted
    ) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        return ResponseEntity.ok(
                demandeService.getByParoissePaged(paroissePublicId, safePage, safeSize, includeDeleted)
        );
    }

    @GetMapping("/paroisse/{paroissePublicId}/statut/{statutDemande}")
    public ResponseEntity<List<DemandeResponse>> getByParoisseAndStatut(
            @PathVariable UUID paroissePublicId,
            @PathVariable StatutDemandeEnum statutDemande
    ) {
        return ResponseEntity.ok(demandeService.getByParoisseAndStatut(paroissePublicId, statutDemande));
    }

    @GetMapping("/paroisse/{paroissePublicId}/supprimees")
    public ResponseEntity<List<DemandeResponse>> getDeletedByParoisse(
            @PathVariable UUID paroissePublicId
    ) {
        return ResponseEntity.ok(demandeService.getDeletedByParoisse(paroissePublicId));
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<Void> delete(@PathVariable UUID publicId) {
        demandeService.deleteByPublicId(publicId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/type-paiement/{typePaiementPublicId}")
    public List<DemandeResponse> getByTypePaiement(@PathVariable UUID typePaiementPublicId) {
        return demandeService.getByTypePaiement(typePaiementPublicId);
    }
}
