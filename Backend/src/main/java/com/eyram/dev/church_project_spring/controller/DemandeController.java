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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    @PreAuthorize("hasAuthority('demand:edit')")
    public ResponseEntity<DemandeResponse> update(
            @PathVariable UUID publicId,
            @Valid @RequestBody DemandeRequest request
    ) {
        return ResponseEntity.ok(demandeService.update(publicId, request));
    }

    @PatchMapping("/{publicId}/validation")
    @PreAuthorize("hasAuthority('demand:validate')")
    public ResponseEntity<DemandeResponse> updateValidation(
            @PathVariable UUID publicId,
            @Valid @RequestBody DemandeValidationRequest request
    ) {
        return ResponseEntity.ok(demandeService.updateValidation(publicId, request));
    }

    @PatchMapping("/{publicId}/intention")
    @PreAuthorize("hasAuthority('demand:edit')")
    public ResponseEntity<DemandeResponse> updateIntention(
            @PathVariable UUID publicId,
            @Valid @RequestBody DemandeIntentionRequest request
    ) {
        return ResponseEntity.ok(demandeService.updateIntention(publicId, request));
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('demand:read')")
    public ResponseEntity<DemandeResponse> getByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(demandeService.getByPublicId(publicId));
    }

    @GetMapping("/code/{codeSuivie}")
    public ResponseEntity<DemandePublicResponse> getByCodeSuivie(@PathVariable String codeSuivie) {
        return ResponseEntity.ok(publicDemandeViewService.toPublic(demandeService.getByCodeSuivie(codeSuivie)));
    }

    @PostMapping("/suivi/par-telephone")
    public ResponseEntity<TrackingByPhoneChallengeResponse> lookupByPhone(
            @Valid @RequestBody TrackingByPhoneRequest request
    ) {
        return ResponseEntity.ok(trackingPhoneOtpService.requestOtp(request.telephone()));
    }

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

    @GetMapping(value = {"/code/{codeSuivie}/recu", "/code/{codeSuivie}/recu.pdf"},
            produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable String codeSuivie) {
        byte[] pdf = demandeReceiptService.generate(codeSuivie);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"recu-" + codeSuivie + ".pdf\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store, private")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    /** Audit plateforme transversal : compte global + permission d'audit. */
    @GetMapping
    @PreAuthorize("hasAuthority('demand:audit') and principal.isGlobal()")
    public ResponseEntity<PageResponse<DemandeResponse>> getAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "true") boolean includeDeleted
    ) {
        return ResponseEntity.ok(demandeService.getAllPaged(page, size, includeDeleted));
    }

    @GetMapping("/paroisse/{paroissePublicId}/stats")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public ResponseEntity<DemandeParoisseStatsResponse> getParoisseStats(
            @PathVariable UUID paroissePublicId
    ) {
        return ResponseEntity.ok(demandeService.getParoisseStats(paroissePublicId));
    }

    @GetMapping("/paroisse/{paroissePublicId}")
    @PreAuthorize("hasAuthority('demand:read')")
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
    @PreAuthorize("hasAuthority('demand:read')")
    public ResponseEntity<List<DemandeResponse>> getByParoisseAndStatut(
            @PathVariable UUID paroissePublicId,
            @PathVariable StatutDemandeEnum statutDemande
    ) {
        return ResponseEntity.ok(demandeService.getByParoisseAndStatut(paroissePublicId, statutDemande));
    }

    @GetMapping("/paroisse/{paroissePublicId}/supprimees")
    @PreAuthorize("hasAuthority('demand:audit')")
    public ResponseEntity<List<DemandeResponse>> getDeletedByParoisse(
            @PathVariable UUID paroissePublicId
    ) {
        return ResponseEntity.ok(demandeService.getDeletedByParoisse(paroissePublicId));
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasAuthority('demand:delete')")
    public ResponseEntity<Void> delete(@PathVariable UUID publicId) {
        demandeService.deleteByPublicId(publicId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/type-paiement/{typePaiementPublicId}")
    @PreAuthorize("hasAuthority('demand:read')")
    public List<DemandeResponse> getByTypePaiement(@PathVariable UUID typePaiementPublicId) {
        return demandeService.getByTypePaiement(typePaiementPublicId);
    }
}
