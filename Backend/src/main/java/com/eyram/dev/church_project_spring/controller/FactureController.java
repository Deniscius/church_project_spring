package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.FactureRequest;
import com.eyram.dev.church_project_spring.DTO.response.FactureResponse;
import com.eyram.dev.church_project_spring.service.FactureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/facture")
@RequiredArgsConstructor
public class FactureController {

    private final FactureService factureService;

    @PostMapping
    public FactureResponse create(@Valid @RequestBody FactureRequest request) {
        return factureService.create(request);
    }

    @GetMapping("/{publicId}")
    public FactureResponse getByPublicId(@PathVariable UUID publicId) {
        return factureService.getByPublicId(publicId);
    }

    @GetMapping("/code-suivie/{codeSuivie}")
    public FactureResponse getByCodeSuivie(@PathVariable String codeSuivie) {
        return factureService.getByCodeSuivie(codeSuivie);
    }

    @GetMapping
    public List<FactureResponse> getAll() {
        return factureService.getAll();
    }

    @PutMapping("/{publicId}")
    public FactureResponse update(@PathVariable UUID publicId,
                                  @Valid @RequestBody FactureRequest request) {
        return factureService.update(publicId, request);
    }

    @DeleteMapping("/{publicId}")
    public void delete(@PathVariable UUID publicId) {
        factureService.delete(publicId);
    }
}