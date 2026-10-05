package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.StructurePastoraleRequest;
import com.eyram.dev.church_project_spring.DTO.request.StructurePastoraleStatutRequest;
import com.eyram.dev.church_project_spring.DTO.response.StructurePastoraleResponse;
import com.eyram.dev.church_project_spring.service.StructurePastoraleService;
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
@RequestMapping("/structures-pastorales")
@RequiredArgsConstructor
public class StructurePastoraleController {

    private final StructurePastoraleService service;

    @PostMapping
    @PreAuthorize("hasAuthority('pastoral-organization:manage')")
    public ResponseEntity<StructurePastoraleResponse> create(
            @Valid @RequestBody StructurePastoraleRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('pastoral-organization:read')")
    public ResponseEntity<StructurePastoraleResponse> getByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getByPublicId(publicId));
    }

    @GetMapping("/paroisse/{paroissePublicId}")
    @PreAuthorize("hasAuthority('pastoral-organization:read')")
    public ResponseEntity<List<StructurePastoraleResponse>> getByParoisse(
            @PathVariable UUID paroissePublicId
    ) {
        return ResponseEntity.ok(service.getByParoisse(paroissePublicId));
    }

    @PutMapping("/{publicId}")
    @PreAuthorize("hasAuthority('pastoral-organization:manage')")
    public ResponseEntity<StructurePastoraleResponse> update(
            @PathVariable UUID publicId,
            @Valid @RequestBody StructurePastoraleRequest request
    ) {
        return ResponseEntity.ok(service.update(publicId, request));
    }

    @PostMapping("/{publicId}/statut")
    @PreAuthorize("hasAuthority('pastoral-organization:manage')")
    public ResponseEntity<StructurePastoraleResponse> setActif(
            @PathVariable UUID publicId,
            @Valid @RequestBody StructurePastoraleStatutRequest request
    ) {
        return ResponseEntity.ok(service.setActif(publicId, request.actif(), request.version()));
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
