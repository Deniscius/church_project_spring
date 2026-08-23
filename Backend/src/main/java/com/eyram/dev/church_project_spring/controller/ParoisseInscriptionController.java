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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
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
    @PreAuthorize("hasAuthority('parish-registration:read') and principal.isGlobal()")
    public ResponseEntity<List<ParoisseInscriptionResponse>> list() {
        return ResponseEntity.ok(inscriptionService.listAll());
    }

    @GetMapping("/{publicId}/documents/{type}")
    @PreAuthorize("hasAuthority('parish-registration:read') and principal.isGlobal()")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable UUID publicId,
            @PathVariable String type
    ) {
        Resource resource = inscriptionService.loadDocument(publicId, type);
        String contentType = inscriptionService.documentContentType(publicId, type);
        String extension = switch (contentType) {
            case MediaType.APPLICATION_PDF_VALUE -> ".pdf";
            case MediaType.IMAGE_PNG_VALUE -> ".png";
            default -> ".jpg";
        };
        String normalizedType = type == null ? "" : type.toLowerCase();
        String filename = normalizedType.contains("cni")
                ? "piece-identite-administrateur" + extension
                : "mandat-cure" + extension;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @PostMapping("/{publicId}/approuver")
    @PreAuthorize("hasAuthority('parish-registration:manage') and principal.isGlobal()")
    public ResponseEntity<Map<String, Object>> approuver(@PathVariable UUID publicId) {
        return ResponseEntity.ok(inscriptionService.approuver(publicId));
    }

    @PostMapping("/{publicId}/rejeter")
    @PreAuthorize("hasAuthority('parish-registration:manage') and principal.isGlobal()")
    public ResponseEntity<ParoisseInscriptionResponse> rejeter(
            @PathVariable UUID publicId,
            @RequestParam(required = false) String motif
    ) {
        return ResponseEntity.ok(inscriptionService.rejeter(publicId, motif));
    }
}
