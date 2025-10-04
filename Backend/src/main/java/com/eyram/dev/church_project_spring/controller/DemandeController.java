package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.DemandeRequest;
import com.eyram.dev.church_project_spring.DTO.response.DemandeResponse;
import com.eyram.dev.church_project_spring.enums.StatusValidationEnum;
import com.eyram.dev.church_project_spring.service.DemandeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/demandes")
public class DemandeController {

    private final DemandeService demandeService;

    public DemandeController(DemandeService demandeService) {
        this.demandeService = demandeService;
    }

    @PostMapping
    public ResponseEntity<DemandeResponse> create(@Valid @RequestBody DemandeRequest demande) {
        DemandeResponse response = demandeService.create(demande);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<DemandeResponse> getByPublicId(@PathVariable UUID publicId) {
        DemandeResponse response = demandeService.getByPublicId(publicId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<DemandeResponse>> getAll() {
        List<DemandeResponse> demandes = demandeService.getAll();
        return ResponseEntity.ok(demandes);
    }

    @GetMapping("/by-client/{clientPublicId}")
    public ResponseEntity<List<DemandeResponse>> getAllByClient(@PathVariable UUID clientPublicId) {
        List<DemandeResponse> demandes = demandeService.getAllByClient(clientPublicId);
        return ResponseEntity.ok(demandes);
    }

    @GetMapping("/by-status/{status}")
    public ResponseEntity<List<DemandeResponse>> getAllByStatus(@PathVariable("status") StatusValidationEnum status) {
        List<DemandeResponse> demandes = demandeService.getAllByStatus(status);
        return ResponseEntity.ok(demandes);
    }

    @PutMapping("/{publicId}")
    public ResponseEntity<DemandeResponse> update(@PathVariable UUID publicId,
                                                  @Valid @RequestBody DemandeRequest demande) {
        DemandeResponse response = demandeService.update(publicId, demande);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/validate/{publicId}")
    public ResponseEntity<DemandeResponse> validateDemande(@PathVariable UUID publicId) {
        DemandeResponse response = demandeService.validateDemande(publicId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/cancel/{publicId}")
    public ResponseEntity<DemandeResponse> cancelDemande(@PathVariable UUID publicId) {
        DemandeResponse response = demandeService.cancelDemande(publicId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable UUID publicId) {
        demandeService.delete(publicId);
        return ResponseEntity.ok(Map.of("message", "Demande supprimée"));
    }
}
