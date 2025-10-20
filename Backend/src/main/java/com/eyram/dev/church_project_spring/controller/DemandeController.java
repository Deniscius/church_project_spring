package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.DemandeRequest;
import com.eyram.dev.church_project_spring.DTO.response.DemandeResponse;
import com.eyram.dev.church_project_spring.enums.StatusValidationEnum;
import com.eyram.dev.church_project_spring.service.DemandeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/demandes")
@RequiredArgsConstructor
public class DemandeController {

    private final DemandeService demandeService;

    @PostMapping
    public ResponseEntity<DemandeResponse> create(@Valid @RequestBody DemandeRequest demande) {
        DemandeResponse response = demandeService.create(demande);
        // Location: /demandes/{publicId}
        return ResponseEntity
                .created(URI.create("/demandes/" + response.publicId()))
                .body(response);
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<DemandeResponse> getByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(demandeService.getByPublicId(publicId));
    }

    // -------- Variante A: ta version sans pagination --------
    @GetMapping
    public ResponseEntity<List<DemandeResponse>> getAll() {
        return ResponseEntity.ok(demandeService.getAll());
    }

    @GetMapping(params = "fidelePublicId")
    public ResponseEntity<List<DemandeResponse>> getAllByFidele(@RequestParam UUID fidelePublicId) {
        return ResponseEntity.ok(demandeService.getAllByFidele(fidelePublicId));
    }

    @GetMapping(params = "status")
    public ResponseEntity<List<DemandeResponse>> getAllByStatus(@RequestParam StatusValidationEnum status) {
        return ResponseEntity.ok(demandeService.getAllByStatus(status));
    }

    // -------- Variante B: endpoint unique avec pagination & filtres (optionnel) --------
    // @GetMapping("/search")
    // public ResponseEntity<Page<DemandeResponse>> search(
    //         @RequestParam(required = false) UUID fidelePublicId,
    //         @RequestParam(required = false) StatusValidationEnum status,
    //         @PageableDefault(size = 20, sort = "dateDemande") Pageable pageable) {
    //     Page<DemandeResponse> page = demandeService.search(fidelePublicId, status, pageable);
    //     return ResponseEntity.ok(page);
    // }

    @PutMapping("/{publicId}")
    public ResponseEntity<DemandeResponse> update(@PathVariable UUID publicId,
                                                  @Valid @RequestBody DemandeRequest demande) {
        return ResponseEntity.ok(demandeService.update(publicId, demande));
    }

    @PatchMapping("/validate/{publicId}")
    public ResponseEntity<DemandeResponse> validateDemande(@PathVariable UUID publicId) {
        return ResponseEntity.ok(demandeService.validateDemande(publicId));
    }

    @PatchMapping("/cancel/{publicId}")
    public ResponseEntity<DemandeResponse> cancelDemande(@PathVariable UUID publicId) {
        return ResponseEntity.ok(demandeService.cancelDemande(publicId));
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable UUID publicId) {
        demandeService.delete(publicId);
        return ResponseEntity.ok(Map.of("message", "Demande supprimée"));
    }
}
