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
    private final com.eyram.dev.church_project_spring.service.desktop.DesktopRevisionService revisions;
    private final com.eyram.dev.church_project_spring.service.DemandeReceiptService receipts;
    public DesktopController(DesktopOperationService operations, com.eyram.dev.church_project_spring.service.DemandeReceiptService receipts,com.eyram.dev.church_project_spring.service.desktop.DesktopRevisionService revisions) { this.operations=operations; this.receipts=receipts;this.revisions=revisions; }
    @GetMapping("/paroisses/{parishId}/receipt-branding")
    @PreAuthorize("hasAnyAuthority('demand:read', 'invoice:read', 'receipt:manage')")
    public com.eyram.dev.church_project_spring.service.DemandeReceiptService.Branding branding(@PathVariable java.util.UUID parishId) {
        return receipts.branding(parishId);
    }
    @GetMapping("/paroisses/{parishId}/revision")
    @PreAuthorize("isAuthenticated()")
    public org.springframework.http.ResponseEntity<java.util.Map<String,String>> revision(@PathVariable java.util.UUID parishId,org.springframework.web.context.request.WebRequest request){
        String revision=revisions.revision(parishId);
        if(request.checkNotModified("\""+revision+"\""))return org.springframework.http.ResponseEntity.status(304).build();
        return org.springframework.http.ResponseEntity.ok().eTag(revision).cacheControl(org.springframework.http.CacheControl.noCache().cachePrivate()).body(java.util.Map.of("revision",revision));
    }
    @GetMapping("/capabilities")
    @PreAuthorize("isAuthenticated()")
    public java.util.Map<String,Integer> capabilities() { return java.util.Map.of("parishJournalVersion",1); }
    @PostMapping("/operations")
    @PreAuthorize("isAuthenticated()")
    public JsonNode execute(@Valid @RequestBody DesktopOperationService.Operation operation) throws Exception {
        return operations.execute(operation);
    }
}
