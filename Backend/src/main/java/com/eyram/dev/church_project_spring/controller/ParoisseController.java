package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseCoordonneesRequest;
import com.eyram.dev.church_project_spring.DTO.request.ParoisseRequest;
import com.eyram.dev.church_project_spring.DTO.response.AnnuaireParoisseResponse;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseResponse;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/paroisses")
@RequiredArgsConstructor
@Validated
public class ParoisseController {

    private final ParoisseService paroisseService;

    @PostMapping
    public ResponseEntity<ParoisseResponse> create(@Valid @RequestBody ParoisseRequest request) {
        ParoisseResponse response = paroisseService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ParoisseResponse>> getAll() {
        List<ParoisseResponse> responses = paroisseService.getAll();
        return ResponseEntity.ok(responses);
    }

    /**
     * Déclaré avant {@code /{publicId}} pour rester lisible : Spring privilégie
     * de toute façon le segment littéral sur la variable de chemin.
     */
    @GetMapping("/annuaire")
    public ResponseEntity<List<AnnuaireParoisseResponse>> getAnnuaire(@RequestParam UUID doyenne) {
        return ResponseEntity.ok(paroisseService.getAnnuaireDisponible(doyenne));
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<ParoisseResponse> getByPublicId(@PathVariable UUID publicId) {
        ParoisseResponse response = paroisseService.getByPublicId(publicId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{publicId}")
    public ResponseEntity<ParoisseResponse> update(
            @PathVariable UUID publicId,
            @Valid @RequestBody ParoisseRequest request
    ) {
        ParoisseResponse response = paroisseService.update(publicId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Coordonnées tenues par la paroisse elle-même : sans RIB, aucune demande
     * de reversement n'est possible, et l'attente d'une intervention du super
     * admin bloquait tout le circuit.
     */
    @PatchMapping("/{publicId}/coordonnees")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ParoisseResponse> updateCoordonnees(
            @PathVariable UUID publicId,
            @Valid @RequestBody ParoisseCoordonneesRequest request
    ) {
        return ResponseEntity.ok(paroisseService.updateCoordonnees(publicId, request));
    }

    @PostMapping(value = "/{publicId}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ParoisseResponse> uploadLogo(
            @PathVariable UUID publicId,
            @RequestParam("logo") MultipartFile logo
    ) {
        return ResponseEntity.ok(paroisseService.updateLogo(publicId, logo));
    }

    @DeleteMapping("/{publicId}/logo")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ParoisseResponse> removeLogo(@PathVariable UUID publicId) {
        return ResponseEntity.ok(paroisseService.removeLogo(publicId));
    }

    @GetMapping("/{publicId}/logo")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETAIRE', 'CURE', 'COMPTABLE_LOCAL', 'SUPER_ADMIN')")
    public ResponseEntity<Resource> getLogo(@PathVariable UUID publicId) {
        Resource resource = paroisseService.loadLogo(publicId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"logo\"")
                .contentType(MediaType.parseMediaType(paroisseService.logoContentType(publicId)))
                .body(resource);
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<Void> deleteByPublicId(@PathVariable UUID publicId) {
        paroisseService.deleteByPublicId(publicId);
        return ResponseEntity.noContent().build();
    }
}
