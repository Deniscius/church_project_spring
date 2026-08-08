package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.DemandeIntentionRequest;
import com.eyram.dev.church_project_spring.DTO.request.DemandeRequest;
import com.eyram.dev.church_project_spring.DTO.request.DemandeTypePaiementRequest;
import com.eyram.dev.church_project_spring.DTO.request.DemandeValidationRequest;
import com.eyram.dev.church_project_spring.DTO.request.TrackingByPhoneRequest;
import com.eyram.dev.church_project_spring.DTO.response.DemandeParoisseStatsResponse;
import com.eyram.dev.church_project_spring.DTO.response.DemandeResponse;
import com.eyram.dev.church_project_spring.DTO.response.PageResponse;
import com.eyram.dev.church_project_spring.DTO.response.TrackingByPhoneResponse;
import com.eyram.dev.church_project_spring.enums.StatutDemandeEnum;
import com.eyram.dev.church_project_spring.service.DemandeReceiptService;
import com.eyram.dev.church_project_spring.service.DemandeService;
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
    public ResponseEntity<DemandeResponse> getByCodeSuivie(@PathVariable String codeSuivie) {
        return ResponseEntity.ok(demandeService.getByCodeSuivie(codeSuivie));
    }

    /**
     * Recherche publique par téléphone : renvoie uniquement les codes de suivi.
     */
    @PostMapping("/suivi/par-telephone")
    public ResponseEntity<TrackingByPhoneResponse> lookupByPhone(
            @Valid @RequestBody TrackingByPhoneRequest request
    ) {
        return ResponseEntity.ok(demandeService.findTrackingCodesByPhone(request.telephone()));
    }

    /** Public : le fidèle peut changer de mode tant que la demande n'est pas payée. */
    @PatchMapping("/code/{codeSuivie}/type-paiement")
    public ResponseEntity<DemandeResponse> updateTypePaiementByCode(
            @PathVariable String codeSuivie,
            @Valid @RequestBody DemandeTypePaiementRequest request
    ) {
        return ResponseEntity.ok(
                demandeService.updateTypePaiementByCodeSuivie(codeSuivie, request.typePaiementPublicId())
        );
    }

    @GetMapping(value = "/code/{codeSuivie}/recu.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable String codeSuivie) {
        byte[] pdf = demandeReceiptService.generate(codeSuivie);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"recu-" + codeSuivie + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<DemandeResponse>> getAll() {
        return ResponseEntity.ok(demandeService.getAll());
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
     */
    @GetMapping("/paroisse/{paroissePublicId}")
    public ResponseEntity<PageResponse<DemandeResponse>> getByParoisse(
            @PathVariable UUID paroissePublicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        return ResponseEntity.ok(
                demandeService.getByParoissePaged(paroissePublicId, safePage, safeSize)
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
