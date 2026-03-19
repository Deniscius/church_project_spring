package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.ForfaitTarifRequest;
import com.eyram.dev.church_project_spring.DTO.response.ForfaitTarifResponse;
import com.eyram.dev.church_project_spring.service.ForfaitTarifService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/forfait-tarifs")
@RequiredArgsConstructor
public class ForfaitTarifController {

    private final ForfaitTarifService forfaitTarifService;

    @PostMapping
    public ResponseEntity<ForfaitTarifResponse> create(@Valid @RequestBody ForfaitTarifRequest request) {
        return new ResponseEntity<>(forfaitTarifService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{publicId}")
    public ResponseEntity<ForfaitTarifResponse> update(@PathVariable UUID publicId,
                                                       @Valid @RequestBody ForfaitTarifRequest request) {
        return ResponseEntity.ok(forfaitTarifService.update(publicId, request));
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<ForfaitTarifResponse> getByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(forfaitTarifService.getByPublicId(publicId));
    }

    @GetMapping
    public ResponseEntity<List<ForfaitTarifResponse>> getAll() {
        return ResponseEntity.ok(forfaitTarifService.getAll());
    }

    @GetMapping("/type-demande/{typeDemandePublicId}")
    public ResponseEntity<List<ForfaitTarifResponse>> getByTypeDemande(@PathVariable UUID typeDemandePublicId) {
        return ResponseEntity.ok(forfaitTarifService.getByTypeDemande(typeDemandePublicId));
    }

    @GetMapping("/type-demande/{typeDemandePublicId}/actifs")
    public ResponseEntity<List<ForfaitTarifResponse>> getActiveByTypeDemande(@PathVariable UUID typeDemandePublicId) {
        return ResponseEntity.ok(forfaitTarifService.getActiveByTypeDemande(typeDemandePublicId));
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<Void> delete(@PathVariable UUID publicId) {
        forfaitTarifService.deleteByPublicId(publicId);
        return ResponseEntity.noContent().build();
    }
}