package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.HoraireRequest;
import com.eyram.dev.church_project_spring.DTO.response.HoraireResponse;
import com.eyram.dev.church_project_spring.service.HoraireService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/horaires")
@RequiredArgsConstructor
public class HoraireController {

    private final HoraireService horaireService;

    // CRÉER
    @PostMapping
    public ResponseEntity<HoraireResponse> create(@RequestBody @Valid HoraireRequest request) {
        HoraireResponse created = horaireService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // METTRE À JOUR
    @PutMapping("/{publicId}")
    public ResponseEntity<HoraireResponse> update(
            @PathVariable UUID publicId,
            @RequestBody @Valid HoraireRequest request
    ) {
        HoraireResponse updated = horaireService.update(publicId, request);
        return ResponseEntity.ok(updated);
    }

    // LISTER (actifs selon l’implémentation du service)
    @GetMapping
    public ResponseEntity<List<HoraireResponse>> getAll() {
        return ResponseEntity.ok(horaireService.getAll());
    }

    // RÉCUPÉRER PAR publicId
    @GetMapping("/{publicId}")
    public ResponseEntity<HoraireResponse> getByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(horaireService.getByPublicId(publicId));
    }

    // SUPPRIMER (soft delete via service)
    @DeleteMapping("/{publicId}")
    public ResponseEntity<Void> delete(@PathVariable UUID publicId) {
        horaireService.delete(publicId);
        return ResponseEntity.noContent().build();
    }
}
