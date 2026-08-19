package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.HoraireRequest;
import com.eyram.dev.church_project_spring.DTO.request.ProgrammeJourUpdateRequest;
import com.eyram.dev.church_project_spring.DTO.response.HoraireResponse;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseHorairesPublicResponse;
import com.eyram.dev.church_project_spring.DTO.response.ProgrammeJourResponse;
import com.eyram.dev.church_project_spring.service.HoraireService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    /** Programme résolu pour une période : défaut hebdomadaire + exceptions de date. */
    @GetMapping("/paroisse/{paroissePublicId}/programme")
    public ResponseEntity<List<ProgrammeJourResponse>> getProgramme(
            @PathVariable UUID paroissePublicId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin
    ) {
        return ResponseEntity.ok(horaireService.getProgramme(paroissePublicId, debut, fin));
    }

    /**
     * Personnalise tous les créneaux disponibles d'une date sans modifier la grille
     * hebdomadaire des autres semaines.
     */
    @PutMapping("/paroisse/{paroissePublicId}/programme/{date}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPTABLE')")
    public ResponseEntity<ProgrammeJourResponse> updateProgrammeForDate(
            @PathVariable UUID paroissePublicId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody ProgrammeJourUpdateRequest request
    ) {
        return ResponseEntity.ok(horaireService.updateProgrammeForDate(paroissePublicId, date, request));
    }

    /**
     * Supprime toutes les exceptions de la date et remet le programme hebdomadaire
     * comme source de vérité pour cette journée.
     */
    @DeleteMapping("/paroisse/{paroissePublicId}/programme/{date}/personnalisation")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPTABLE')")
    public ResponseEntity<ProgrammeJourResponse> resetProgrammeForDate(
            @PathVariable UUID paroissePublicId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(horaireService.resetProgrammeForDate(paroissePublicId, date));
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
