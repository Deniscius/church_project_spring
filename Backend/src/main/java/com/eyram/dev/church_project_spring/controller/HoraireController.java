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

    @PostMapping
    public ResponseEntity<HoraireResponse> create(@Valid @RequestBody HoraireRequest request) {
        HoraireResponse response = horaireService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<HoraireResponse>> getAll() {
        return ResponseEntity.ok(horaireService.getAll());
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<HoraireResponse> getByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(horaireService.getByPublicId(publicId));
    }

    @GetMapping("/paroisse/{paroissePublicId}")
    public ResponseEntity<List<HoraireResponse>> getByParoisse(@PathVariable UUID paroissePublicId) {
        return ResponseEntity.ok(horaireService.getByParoisse(paroissePublicId));
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
