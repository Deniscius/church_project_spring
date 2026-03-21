package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.DemandeRequest;
import com.eyram.dev.church_project_spring.DTO.response.DemandeResponse;
import com.eyram.dev.church_project_spring.enums.StatutDemandeEnum;
import com.eyram.dev.church_project_spring.service.DemandeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/demandes")
@RequiredArgsConstructor
public class DemandeController {

    private final DemandeService demandeService;

    @PostMapping
    public ResponseEntity<DemandeResponse> create(@Valid @RequestBody DemandeRequest request) {
        return new ResponseEntity<>(demandeService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{publicId}")
    public ResponseEntity<DemandeResponse> update(@PathVariable UUID publicId,
                                                  @Valid @RequestBody DemandeRequest request) {
        return ResponseEntity.ok(demandeService.update(publicId, request));
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<DemandeResponse> getByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(demandeService.getByPublicId(publicId));
    }

    @GetMapping("/code/{codeSuivie}")
    public ResponseEntity<DemandeResponse> getByCodeSuivie(@PathVariable String codeSuivie) {
        return ResponseEntity.ok(demandeService.getByCodeSuivie(codeSuivie));
    }

    @GetMapping
    public ResponseEntity<List<DemandeResponse>> getAll() {
        return ResponseEntity.ok(demandeService.getAll());
    }

    @GetMapping("/paroisse/{paroissePublicId}")
    public ResponseEntity<List<DemandeResponse>> getByParoisse(@PathVariable UUID paroissePublicId) {
        return ResponseEntity.ok(demandeService.getByParoisse(paroissePublicId));
    }

    @GetMapping("/paroisse/{paroissePublicId}/statut/{statutDemande}")
    public ResponseEntity<List<DemandeResponse>> getByParoisseAndStatut(
            @PathVariable UUID paroissePublicId,
            @PathVariable StatutDemandeEnum statutDemande
    ) {
        return ResponseEntity.ok(demandeService.getByParoisseAndStatut(paroissePublicId, statutDemande));
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<Void> delete(@PathVariable UUID publicId) {
        demandeService.deleteByPublicId(publicId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/type-paiement/{typePaiementPublicId}")
    public List<DemandeResponse> getByTypePaiement(@PathVariable UUID typePaiementPublicId) {
        return demandeService.getByTypePaiement(typePaiementPublicId);
    }
}