package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.service.desktop.DesktopOperationService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/desktop")
public class DesktopController {
    private final DesktopOperationService operations;
    private final com.eyram.dev.church_project_spring.service.DemandeReceiptService receipts;
    public DesktopController(DesktopOperationService operations, com.eyram.dev.church_project_spring.service.DemandeReceiptService receipts) { this.operations=operations; this.receipts=receipts; }
    @GetMapping("/paroisses/{parishId}/receipt-branding")
    @PreAuthorize("hasAnyAuthority('demand:read', 'invoice:read', 'receipt:manage')")
    public com.eyram.dev.church_project_spring.service.DemandeReceiptService.Branding branding(@PathVariable java.util.UUID parishId) {
        return receipts.branding(parishId);
    }
    @PostMapping("/operations")
    @PreAuthorize("isAuthenticated()")
    public JsonNode execute(@Valid @RequestBody DesktopOperationService.Operation operation) throws Exception {
        return operations.execute(operation);
    }
}
