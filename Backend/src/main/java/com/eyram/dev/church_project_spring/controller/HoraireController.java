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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/horaires")
@RequiredArgsConstructor
public class HoraireController {

    private final HoraireService horaireService;

    @PostMapping
    @PreAuthorize("hasAuthority('schedule:manage')")
    public ResponseEntity<HoraireResponse> create(@Valid @RequestBody HoraireRequest request) {
        HoraireResponse response = horaireService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('schedule:read')")
    public ResponseEntity<List<HoraireResponse>> getAll() {
        return ResponseEntity.ok(horaireService.getAll());
    }

    @GetMapping("/public/paroisses-actives")
    public ResponseEntity<List<ParoisseHorairesPublicResponse>> listPublicForActiveParishes() {
        return ResponseEntity.ok(horaireService.listPublicHorairesForActiveParishes());
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('schedule:read')")
    public ResponseEntity<HoraireResponse> getByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(horaireService.getByPublicId(publicId));
    }

    /** Public : horaires actifs d'une paroisse. */
    @GetMapping("/paroisse/{paroissePublicId}")
    public ResponseEntity<List<HoraireResponse>> getByParoisse(@PathVariable UUID paroissePublicId) {
        return ResponseEntity.ok(horaireService.getByParoisse(paroissePublicId));
    }

    /** Public : programme résolu (défaut hebdomadaire + exceptions de date). */
    @GetMapping("/paroisse/{paroissePublicId}/programme")
    public ResponseEntity<List<ProgrammeJourResponse>> getProgramme(
            @PathVariable UUID paroissePublicId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin
    ) {
        return ResponseEntity.ok(horaireService.getProgramme(paroissePublicId, debut, fin));
    }

    @PutMapping("/paroisse/{paroissePublicId}/programme/{date}")
    @PreAuthorize("hasAuthority('schedule:manage')")
    public ResponseEntity<ProgrammeJourResponse> updateProgrammeForDate(
            @PathVariable UUID paroissePublicId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody ProgrammeJourUpdateRequest request
    ) {
        return ResponseEntity.ok(horaireService.updateProgrammeForDate(paroissePublicId, date, request));
    }

    @DeleteMapping("/paroisse/{paroissePublicId}/programme/{date}/personnalisation")
    @PreAuthorize("hasAuthority('schedule:manage')")
    public ResponseEntity<ProgrammeJourResponse> resetProgrammeForDate(
            @PathVariable UUID paroissePublicId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(horaireService.resetProgrammeForDate(paroissePublicId, date));
    }

    @PutMapping("/{publicId}")
    @PreAuthorize("hasAuthority('schedule:manage')")
    public ResponseEntity<HoraireResponse> update(
            @PathVariable UUID publicId,
            @Valid @RequestBody HoraireRequest request
    ) {
        return ResponseEntity.ok(horaireService.update(publicId, request));
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasAuthority('schedule:manage')")
    public ResponseEntity<Void> delete(@PathVariable UUID publicId) {
        horaireService.deleteByPublicId(publicId);
        return ResponseEntity.noContent().build();
    }
}
