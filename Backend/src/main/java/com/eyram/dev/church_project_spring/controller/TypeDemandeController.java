package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.TypeDemandeRequest;
import com.eyram.dev.church_project_spring.DTO.response.TypeDemandeResponse;
import com.eyram.dev.church_project_spring.service.TypeDemandeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/type-demandes")
@RequiredArgsConstructor
public class TypeDemandeController {

    private final TypeDemandeService typeDemandeService;

    @PostMapping
    public ResponseEntity<TypeDemandeResponse> create(@Valid @RequestBody TypeDemandeRequest request) {
        TypeDemandeResponse response = typeDemandeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TypeDemandeResponse>> getAll() {
        return ResponseEntity.ok(typeDemandeService.getAll());
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<TypeDemandeResponse> getByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(typeDemandeService.getByPublicId(publicId));
    }

    @GetMapping("/paroisse/{paroissePublicId}")
    public ResponseEntity<List<TypeDemandeResponse>> getByParoisse(@PathVariable UUID paroissePublicId) {
        return ResponseEntity.ok(typeDemandeService.getByParoisse(paroissePublicId));
    }

    @PutMapping("/{publicId}")
    public ResponseEntity<TypeDemandeResponse> update(
            @PathVariable UUID publicId,
            @Valid @RequestBody TypeDemandeRequest request
    ) {
        return ResponseEntity.ok(typeDemandeService.update(publicId, request));
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<Void> delete(@PathVariable UUID publicId) {
        typeDemandeService.deleteByPublicId(publicId);
        return ResponseEntity.noContent().build();
    }
}