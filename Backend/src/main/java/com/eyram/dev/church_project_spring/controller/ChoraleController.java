package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.ChoraleInvitationTokenRequest;
import com.eyram.dev.church_project_spring.DTO.request.ChoraleRequest;
import com.eyram.dev.church_project_spring.DTO.response.ChoraleClaimResponse;
import com.eyram.dev.church_project_spring.DTO.response.ChoraleInvitationResponse;
import com.eyram.dev.church_project_spring.DTO.response.ChoraleResponse;
import com.eyram.dev.church_project_spring.service.ChoraleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ChoraleController {
    private final ChoraleService service;

    @PostMapping("/paroisses/{paroissePublicId}/chorales")
    @PreAuthorize("hasAuthority('choir:manage')")
    public ResponseEntity<ChoraleResponse> create(@PathVariable UUID paroissePublicId, @Valid @RequestBody ChoraleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(paroissePublicId, request));
    }

    @GetMapping("/paroisses/{paroissePublicId}/chorales")
    @PreAuthorize("hasAuthority('choir:read')")
    public List<ChoraleResponse> list(@PathVariable UUID paroissePublicId) { return service.list(paroissePublicId); }

    @GetMapping("/chorales/{publicId}")
    @PreAuthorize("hasAuthority('choir:read')")
    public ChoraleResponse get(@PathVariable UUID publicId) { return service.get(publicId); }

    @PutMapping("/chorales/{publicId}")
    @PreAuthorize("hasAuthority('choir:manage')")
    public ChoraleResponse update(@PathVariable UUID publicId, @Valid @RequestBody ChoraleRequest request) { return service.update(publicId, request); }

    @DeleteMapping("/chorales/{publicId}")
    @PreAuthorize("hasAuthority('choir:manage')")
    public ResponseEntity<Void> delete(@PathVariable UUID publicId) { service.delete(publicId); return ResponseEntity.noContent().build(); }

    @PostMapping("/chorales/{publicId}/invitations")
    @PreAuthorize("hasAuthority('choir:invite')")
    public ResponseEntity<ChoraleInvitationResponse> invite(@PathVariable UUID publicId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.invite(publicId));
    }

    @PostMapping("/integrations/chorale-management/invitations/resolve")
    public ChoraleClaimResponse resolve(@Valid @RequestBody ChoraleInvitationTokenRequest request) {
        return service.resolveInvitation(request.token());
    }

    @PostMapping("/integrations/chorale-management/invitations/consume")
    public ChoraleClaimResponse consume(@Valid @RequestBody ChoraleInvitationTokenRequest request) {
        return service.consumeInvitation(request.token());
    }
}
