package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseCoordonneesRequest;
import com.eyram.dev.church_project_spring.DTO.request.ParoisseRequest;
import com.eyram.dev.church_project_spring.DTO.response.AnnuaireParoisseResponse;
import com.eyram.dev.church_project_spring.DTO.response.ParoissePublicResponse;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseResponse;
import com.eyram.dev.church_project_spring.service.DemandeReceiptService;
import com.eyram.dev.church_project_spring.service.ParoisseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/paroisses")
@RequiredArgsConstructor
@Validated
public class ParoisseController {

    private final ParoisseService paroisseService;
    private final DemandeReceiptService demandeReceiptService;

    @PostMapping
    @PreAuthorize("hasAuthority('parish:manage') and principal.isGlobal()")
    public ResponseEntity<ParoisseResponse> create(@Valid @RequestBody ParoisseRequest request) {
        ParoisseResponse response = paroisseService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** DTO complet : uniquement dans le périmètre autorisé de l'utilisateur. */
    @GetMapping
    @PreAuthorize("hasAuthority('parish:read')")
    public ResponseEntity<List<ParoisseResponse>> getAll() {
        return ResponseEntity.ok(paroisseService.getAll());
    }

    @GetMapping("/public")
    public ResponseEntity<List<ParoissePublicResponse>> listPublicActives() {
        return ResponseEntity.ok(paroisseService.listPublicActives());
    }

    @GetMapping("/annuaire/{doyennePublicId}")
    public ResponseEntity<List<AnnuaireParoisseResponse>> getAnnuaire(@PathVariable UUID doyennePublicId) {
        return ResponseEntity.ok(paroisseService.getAnnuaireDisponible(doyennePublicId));
    }

    @GetMapping("/annuaire")
    public ResponseEntity<List<AnnuaireParoisseResponse>> getAnnuaireQuery(@RequestParam UUID doyenne) {
        return ResponseEntity.ok(paroisseService.getAnnuaireDisponible(doyenne));
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('parish:read')")
    public ResponseEntity<ParoisseResponse> getByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(paroisseService.getByPublicId(publicId));
    }

    @PutMapping("/{publicId}")
    @PreAuthorize("hasAuthority('parish:manage') and principal.isGlobal()")
    public ResponseEntity<ParoisseResponse> update(
            @PathVariable UUID publicId,
            @Valid @RequestBody ParoisseRequest request
    ) {
        return ResponseEntity.ok(paroisseService.update(publicId, request));
    }

    @PatchMapping("/{publicId}/coordonnees")
    @PreAuthorize("hasAuthority('parish-settings:manage')")
    public ResponseEntity<ParoisseResponse> updateCoordonnees(
            @PathVariable UUID publicId,
            @Valid @RequestBody ParoisseCoordonneesRequest request
    ) {
        return ResponseEntity.ok(paroisseService.updateCoordonnees(publicId, request));
    }

    @PostMapping(value = "/{publicId}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('parish-settings:manage')")
    public ResponseEntity<ParoisseResponse> uploadLogo(
            @PathVariable UUID publicId,
            @RequestParam("logo") MultipartFile logo
    ) {
        return ResponseEntity.ok(paroisseService.updateLogo(publicId, logo));
    }

    @DeleteMapping("/{publicId}/logo")
    @PreAuthorize("hasAuthority('parish-settings:manage')")
    public ResponseEntity<ParoisseResponse> removeLogo(@PathVariable UUID publicId) {
        return ResponseEntity.ok(paroisseService.removeLogo(publicId));
    }

    @GetMapping("/{publicId}/logo")
    @PreAuthorize("hasAuthority('parish:read')")
    public ResponseEntity<Resource> getLogo(@PathVariable UUID publicId) {
        Resource resource = paroisseService.loadLogo(publicId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"logo\"")
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=86400")
                .contentType(MediaType.parseMediaType(paroisseService.logoContentType(publicId)))
                .body(resource);
    }

    @GetMapping(value = {"/{publicId}/recu-modele", "/{publicId}/recu-modele.pdf"},
            produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('parish:read')")
    public ResponseEntity<byte[]> previewReceiptSample(@PathVariable UUID publicId) {
        byte[] pdf = demandeReceiptService.generateSampleForParoisse(publicId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"recu-modele.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasAuthority('parish:manage') and principal.isGlobal()")
    public ResponseEntity<Void> deleteByPublicId(@PathVariable UUID publicId) {
        paroisseService.deleteByPublicId(publicId);
        return ResponseEntity.noContent().build();
    }
}
