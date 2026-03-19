package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.DemandeDateRequest;
import com.eyram.dev.church_project_spring.DTO.response.DemandeDateResponse;
import com.eyram.dev.church_project_spring.service.DemandeDateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/demande-dates")
@RequiredArgsConstructor
public class DemandeDateController {

    private final DemandeDateService demandeDateService;

    @PostMapping
    public ResponseEntity<DemandeDateResponse> create(@Valid @RequestBody DemandeDateRequest request) {
        return new ResponseEntity<>(demandeDateService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{publicId}")
    public ResponseEntity<DemandeDateResponse> update(@PathVariable UUID publicId,
                                                      @Valid @RequestBody DemandeDateRequest request) {
        return ResponseEntity.ok(demandeDateService.update(publicId, request));
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<DemandeDateResponse> getByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(demandeDateService.getByPublicId(publicId));
    }

    @GetMapping
    public ResponseEntity<List<DemandeDateResponse>> getAll() {
        return ResponseEntity.ok(demandeDateService.getAll());
    }

    @GetMapping("/demande/{demandePublicId}")
    public ResponseEntity<List<DemandeDateResponse>> getByDemande(@PathVariable UUID demandePublicId) {
        return ResponseEntity.ok(demandeDateService.getByDemande(demandePublicId));
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<Void> delete(@PathVariable UUID publicId) {
        demandeDateService.deleteByPublicId(publicId);
        return ResponseEntity.noContent().build();
    }
}