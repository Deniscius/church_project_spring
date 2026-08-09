package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.FactureRequest;
import com.eyram.dev.church_project_spring.DTO.response.FacturePublicResponse;
import com.eyram.dev.church_project_spring.DTO.response.FactureResponse;
import com.eyram.dev.church_project_spring.service.FactureService;
import com.eyram.dev.church_project_spring.service.PublicDemandeViewService;
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
    private final PublicDemandeViewService publicDemandeViewService;

    @PostMapping
    public FactureResponse create(@Valid @RequestBody FactureRequest request) {
        return factureService.create(request);
    }

    @GetMapping
    public List<FactureResponse> getAll() {
        return factureService.getAll();
    }

    @GetMapping("/paroisse/{paroissePublicId}")
    public List<FactureResponse> getByParoisse(@PathVariable UUID paroissePublicId) {
        return factureService.getByParoisse(paroissePublicId);
    }

    @GetMapping("/code-suivie/{codeSuivie}")
    public FacturePublicResponse getByCodeSuivie(@PathVariable String codeSuivie) {
        return publicDemandeViewService.toPublic(factureService.getByCodeSuivie(codeSuivie));
    }

    @GetMapping("/{publicId}")
    public FactureResponse getByPublicId(@PathVariable UUID publicId) {
        return factureService.getByPublicId(publicId);
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
