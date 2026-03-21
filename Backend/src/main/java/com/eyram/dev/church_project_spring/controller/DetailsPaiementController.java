package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.DetailsPaiementRequest;
import com.eyram.dev.church_project_spring.DTO.response.DetailsPaiementResponse;
import com.eyram.dev.church_project_spring.DTO.response.FactureResponse;
import com.eyram.dev.church_project_spring.service.DetailsPaiementService;
import com.eyram.dev.church_project_spring.service.impl.FactureServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/details-paiement")
@RequiredArgsConstructor
public class DetailsPaiementController {

    private final DetailsPaiementService detailsPaiementService;
    private final FactureServiceImpl factureServiceImpl;

    @PostMapping
    public DetailsPaiementResponse create(@Valid @RequestBody DetailsPaiementRequest request) {
        return detailsPaiementService.create(request);
    }

    @GetMapping("/{publicId}")
    public DetailsPaiementResponse getByPublicId(@PathVariable UUID publicId) {
        return detailsPaiementService.getByPublicId(publicId);
    }

    @GetMapping
    public List<DetailsPaiementResponse> getAll() {
        return detailsPaiementService.getAll();
    }

    @PutMapping("/{publicId}")
    public DetailsPaiementResponse update(@PathVariable UUID publicId,
                                          @Valid @RequestBody DetailsPaiementRequest request) {
        return detailsPaiementService.update(publicId, request);
    }

    @DeleteMapping("/{publicId}")
    public void delete(@PathVariable UUID publicId) {
        detailsPaiementService.delete(publicId);
    }


    @GetMapping("/code-suivie/{codeSuivie}")
    public FactureResponse getByCodeSuivie(@PathVariable String codeSuivie) {
        return factureServiceImpl.getByCodeSuivie(codeSuivie);
    }
}