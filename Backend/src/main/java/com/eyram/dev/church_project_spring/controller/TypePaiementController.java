package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.TypePaiementRequest;
import com.eyram.dev.church_project_spring.DTO.response.TypePaiementResponse;
import com.eyram.dev.church_project_spring.service.TypePaiementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/type-paiement")
@RequiredArgsConstructor
public class TypePaiementController {

    private final TypePaiementService typePaiementService;

    @PostMapping
    public TypePaiementResponse create(@Valid @RequestBody TypePaiementRequest request) {
        return typePaiementService.create(request);
    }

    @GetMapping("/{publicId}")
    public TypePaiementResponse getByPublicId(@PathVariable UUID publicId) {
        return typePaiementService.getByPublicId(publicId);
    }

    @GetMapping
    public List<TypePaiementResponse> getAll() {
        return typePaiementService.getAll();
    }

    @PutMapping("/{publicId}")
    public TypePaiementResponse update(@PathVariable UUID publicId, @Valid @RequestBody TypePaiementRequest request) {
        return typePaiementService.update(publicId, request);
    }

    @DeleteMapping("/{publicId}")
    public void delete(@PathVariable UUID publicId) {
        typePaiementService.delete(publicId);
    }
}