package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.PlanSaasRequest;
import com.eyram.dev.church_project_spring.DTO.response.PlanSaasResponse;
import com.eyram.dev.church_project_spring.service.PlanSaasService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/plans-saas")
@RequiredArgsConstructor
public class PlanSaasController {

    private final PlanSaasService planSaasService;

    @GetMapping("/public")
    public ResponseEntity<List<PlanSaasResponse>> publicPlans() {
        return ResponseEntity.ok(planSaasService.findPublicActive());
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') and principal.isGlobal()")
    public ResponseEntity<List<PlanSaasResponse>> findAll() {
        return ResponseEntity.ok(planSaasService.findAll());
    }

    @PutMapping("/{publicId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') and principal.isGlobal()")
    public ResponseEntity<PlanSaasResponse> update(
            @PathVariable UUID publicId,
            @Valid @RequestBody PlanSaasRequest request
    ) {
        return ResponseEntity.ok(planSaasService.update(publicId, request));
    }
}
