package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.InscriptionOtpSendRequest;
import com.eyram.dev.church_project_spring.DTO.request.InscriptionOtpVerifyRequest;
import com.eyram.dev.church_project_spring.DTO.request.ParoisseInscriptionRequest;
import com.eyram.dev.church_project_spring.DTO.response.InscriptionOtpVerifyResponse;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseInscriptionResponse;
import com.eyram.dev.church_project_spring.service.billing.InscriptionOtpService;
import com.eyram.dev.church_project_spring.service.billing.ParoisseInscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/inscriptions-paroisse")
@RequiredArgsConstructor
public class ParoisseInscriptionController {

    private final ParoisseInscriptionService inscriptionService;
    private final InscriptionOtpService inscriptionOtpService;

    @PostMapping("/otp/envoyer")
    public ResponseEntity<Map<String, Object>> envoyerOtp(@Valid @RequestBody InscriptionOtpSendRequest request) {
        return ResponseEntity.ok(inscriptionOtpService.sendOtp(request.email()));
    }

    @PostMapping("/otp/verifier")
    public ResponseEntity<InscriptionOtpVerifyResponse> verifierOtp(
            @Valid @RequestBody InscriptionOtpVerifyRequest request
    ) {
        return ResponseEntity.ok(inscriptionOtpService.verifyOtp(
                request.email(),
                request.code(),
                request.adminPrenom(),
                request.adminNom(),
                request.nomParoisse()
        ));
    }

    /**
     * Dossier d'inscription : JSON métier + scans obligatoires
     * (mandat du curé, pièce d'identité du premier admin).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ParoisseInscriptionResponse> soumettre(
            @Valid @RequestPart("payload") ParoisseInscriptionRequest request,
            @RequestPart("mandatCure") MultipartFile mandatCure,
            @RequestPart("adminCni") MultipartFile adminCni
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inscriptionService.soumettre(request, mandatCure, adminCni));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COMPTABLE', 'SUPER_ADMIN')")
    public ResponseEntity<List<ParoisseInscriptionResponse>> list() {
        return ResponseEntity.ok(inscriptionService.listAll());
    }

    @GetMapping("/{publicId}/documents/{type}")
    @PreAuthorize("hasAnyRole('COMPTABLE', 'SUPER_ADMIN')")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable UUID publicId,
            @PathVariable String type
    ) {
        Resource resource = inscriptionService.loadDocument(publicId, type);
        String contentType = inscriptionService.documentContentType(publicId, type);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + type + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @PostMapping("/{publicId}/approuver")
    @PreAuthorize("hasAnyRole('COMPTABLE', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> approuver(@PathVariable UUID publicId) {
        return ResponseEntity.ok(inscriptionService.approuver(publicId));
    }

    @PostMapping("/{publicId}/rejeter")
    @PreAuthorize("hasAnyRole('COMPTABLE', 'SUPER_ADMIN')")
    public ResponseEntity<ParoisseInscriptionResponse> rejeter(
            @PathVariable UUID publicId,
            @RequestParam(required = false) String motif
    ) {
        return ResponseEntity.ok(inscriptionService.rejeter(publicId, motif));
    }
}
