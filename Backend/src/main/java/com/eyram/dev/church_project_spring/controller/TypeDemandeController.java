package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.TypeDemandeRequest;
import com.eyram.dev.church_project_spring.DTO.response.TypeDemandeResponse;
import com.eyram.dev.church_project_spring.service.TypeDemandeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/type-demandes")
public class TypeDemandeController {

    private final TypeDemandeService typeDemandeService;

    public TypeDemandeController(TypeDemandeService typeDemandeService) {
        this.typeDemandeService = typeDemandeService;
    }

    @PostMapping
    public ResponseEntity<TypeDemandeResponse> create(@Valid @RequestBody TypeDemandeRequest typeDemandeRequest) {
        TypeDemandeResponse response = typeDemandeService.create(typeDemandeRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<TypeDemandeResponse> getById(@PathVariable UUID publicId) {
        TypeDemandeResponse response = typeDemandeService.getById(publicId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<TypeDemandeResponse>> getAll() {
        List<TypeDemandeResponse> typeDemandes = typeDemandeService.getAll();
        return ResponseEntity.ok(typeDemandes);
    }

    @PutMapping("/{publicId}")
    public ResponseEntity<TypeDemandeResponse> update(@PathVariable UUID publicId,
                                                      @Valid @RequestBody TypeDemandeRequest typeDemandeRequest) {
        TypeDemandeResponse response = typeDemandeService.update(publicId, typeDemandeRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable UUID publicId) {
        typeDemandeService.delete(publicId);
        return ResponseEntity.ok(Map.of("message", "Type de demande supprimé avec succès"));
    }

    @GetMapping("/admin")
    public ResponseEntity<List<TypeDemandeResponse>> getAllForAdmin() {
        List<TypeDemandeResponse> typeDemandes = typeDemandeService.getAllForAdmin();
        return ResponseEntity.ok(typeDemandes);
    }
}
