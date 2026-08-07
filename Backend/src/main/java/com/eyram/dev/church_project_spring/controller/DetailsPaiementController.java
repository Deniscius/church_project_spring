package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.DetailsPaiementRequest;
import com.eyram.dev.church_project_spring.DTO.response.CaisseResumeResponse;
import com.eyram.dev.church_project_spring.DTO.response.DetailsPaiementResponse;
import com.eyram.dev.church_project_spring.DTO.response.FactureResponse;
import com.eyram.dev.church_project_spring.service.DetailsPaiementService;
import com.eyram.dev.church_project_spring.service.FactureService;
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
    private final FactureService factureService;

    @PostMapping
    public DetailsPaiementResponse create(@Valid @RequestBody DetailsPaiementRequest request) {
        return detailsPaiementService.create(request);
    }

    /**
     * Déclaré avant {@code /{publicId}} : segment littéral prioritaire pour Spring.
     */
    @PostMapping("/caisse/{demandePublicId}")
    public DetailsPaiementResponse encaisserCaisse(@PathVariable UUID demandePublicId) {
        return detailsPaiementService.encaisserCaisse(demandePublicId);
    }

    @GetMapping("/caisse/paroisse/{paroissePublicId}")
    public CaisseResumeResponse resumeCaisse(@PathVariable UUID paroissePublicId) {
        return detailsPaiementService.resumeCaisse(paroissePublicId);
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
        return factureService.getByCodeSuivie(codeSuivie);
    }
}