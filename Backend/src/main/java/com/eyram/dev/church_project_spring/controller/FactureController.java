package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.FactureResquest;
import com.eyram.dev.church_project_spring.DTO.response.FactureResponse;
import com.eyram.dev.church_project_spring.entities.Facture;
import com.eyram.dev.church_project_spring.service.FactureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/factures")
@RequiredArgsConstructor
public class FactureController {

    private final FactureService factureService;


    @PostMapping
    public ResponseEntity<FactureResponse> create(@RequestBody @Valid FactureResquest request) {
        FactureResponse created = factureService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }


    @GetMapping
    public ResponseEntity<List<Facture>> findAll() {
        return ResponseEntity.ok(factureService.findAll());
    }


    @GetMapping("/{publicId}")
    public ResponseEntity<Facture> findByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(factureService.findByPublicId(publicId));
    }


    @PutMapping("/{publicId}")
    public ResponseEntity<FactureResponse> update(
            @PathVariable UUID publicId,
            @RequestBody @Valid FactureResquest request
    ) {
        FactureResponse updated = factureService.update(publicId, request);
        return ResponseEntity.ok(updated);
    }


    @DeleteMapping("/{publicId}")
    public ResponseEntity<Void> delete(@PathVariable UUID publicId) {
        factureService.delete(publicId);
        return ResponseEntity.noContent().build();
    }
}
