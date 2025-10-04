package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.TypePaiementRequest;
import com.eyram.dev.church_project_spring.DTO.response.TypePaiementResponse;
import com.eyram.dev.church_project_spring.service.TypePaiementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/type-paiements")
@RequiredArgsConstructor
public class TypePaiementController {

    private final TypePaiementService typePaiementService;

    @PostMapping
    public ResponseEntity<TypePaiementResponse> create(@RequestBody TypePaiementRequest request) {
        return ResponseEntity.ok(typePaiementService.create(request));
    }

    @PutMapping("/{publicId}")
    public ResponseEntity<TypePaiementResponse> update(@PathVariable UUID publicId,
                                                       @RequestBody TypePaiementRequest request) {
        return ResponseEntity.ok(typePaiementService.update(publicId, request));
    }

    @GetMapping
    public ResponseEntity<List<TypePaiementResponse>> getAll() {
        return ResponseEntity.ok(typePaiementService.findAll());
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<TypePaiementResponse> getById(@PathVariable UUID publicId) {
        return ResponseEntity.ok(typePaiementService.findById(publicId));
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<Void> delete(@PathVariable UUID publicId) {
        typePaiementService.delete(publicId);
        return ResponseEntity.noContent().build();
    }
}
