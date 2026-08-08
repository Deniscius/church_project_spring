package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.HoraireRequest;
import com.eyram.dev.church_project_spring.DTO.response.HoraireResponse;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseHorairesPublicResponse;
import com.eyram.dev.church_project_spring.DTO.response.ProgrammeJourResponse;
import com.eyram.dev.church_project_spring.service.HoraireService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/horaires")
@RequiredArgsConstructor
public class HoraireController {

    private final HoraireService horaireService;

    @PostMapping
    public ResponseEntity<HoraireResponse> create(@Valid @RequestBody HoraireRequest request) {
        HoraireResponse response = horaireService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<HoraireResponse>> getAll() {
        return ResponseEntity.ok(horaireService.getAll());
    }

    /**
     * Accueil public : horaires actifs des paroisses à abonnement actif.
     * Déclaré avant {@code /{publicId}} pour éviter toute collision de mapping.
     */
    @GetMapping("/public/paroisses-actives")
    public ResponseEntity<List<ParoisseHorairesPublicResponse>> listPublicForActiveParishes() {
        return ResponseEntity.ok(horaireService.listPublicHorairesForActiveParishes());
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<HoraireResponse> getByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(horaireService.getByPublicId(publicId));
    }

    @GetMapping("/paroisse/{paroissePublicId}")
    public ResponseEntity<List<HoraireResponse>> getByParoisse(@PathVariable UUID paroissePublicId) {
        return ResponseEntity.ok(horaireService.getByParoisse(paroissePublicId));
    }

    /**
     * Programme résolu (hebdomadaire + dates précises, messe unique respectée).
     * Déclaré avant {@code /{publicId}} n'est pas nécessaire ici (chemin plus spécifique).
     */
    @GetMapping("/paroisse/{paroissePublicId}/programme")
    public ResponseEntity<List<ProgrammeJourResponse>> getProgramme(
            @PathVariable UUID paroissePublicId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin
    ) {
        return ResponseEntity.ok(horaireService.getProgramme(paroissePublicId, debut, fin));
    }

    @PutMapping("/{publicId}")
    public ResponseEntity<HoraireResponse> update(
            @PathVariable UUID publicId,
            @Valid @RequestBody HoraireRequest request
    ) {
        return ResponseEntity.ok(horaireService.update(publicId, request));
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<Void> delete(@PathVariable UUID publicId) {
        horaireService.deleteByPublicId(publicId);
        return ResponseEntity.noContent().build();
    }
}
