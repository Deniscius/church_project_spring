package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.MandatPastoralRequest;
import com.eyram.dev.church_project_spring.DTO.response.MandatPastoralResponse;
import com.eyram.dev.church_project_spring.service.MandatPastoralService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/mandats-pastoraux")
@RequiredArgsConstructor
public class MandatPastoralController {

    private final MandatPastoralService service;

    @PostMapping
    @PreAuthorize("hasAuthority('pastoral-organization:manage')")
    public ResponseEntity<MandatPastoralResponse> create(
            @Valid @RequestBody MandatPastoralRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('pastoral-organization:read')")
    public ResponseEntity<MandatPastoralResponse> getByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getByPublicId(publicId));
    }

    @GetMapping("/paroisse/{paroissePublicId}/annee/{anneePastoralePublicId}")
    @PreAuthorize("hasAuthority('pastoral-organization:read')")
    public ResponseEntity<List<MandatPastoralResponse>> getByAnnee(
            @PathVariable UUID paroissePublicId,
            @PathVariable UUID anneePastoralePublicId
    ) {
        return ResponseEntity.ok(service.getByAnnee(paroissePublicId, anneePastoralePublicId));
    }

    @PutMapping("/{publicId}")
    @PreAuthorize("hasAuthority('pastoral-organization:manage')")
    public ResponseEntity<MandatPastoralResponse> update(
            @PathVariable UUID publicId,
            @Valid @RequestBody MandatPastoralRequest request
    ) {
        return ResponseEntity.ok(service.update(publicId, request));
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasAuthority('pastoral-organization:manage')")
    public ResponseEntity<Void> archive(
            @PathVariable UUID publicId,
            @RequestParam Long version
    ) {
        service.archive(publicId, version);
        return ResponseEntity.noContent().build();
    }
}
