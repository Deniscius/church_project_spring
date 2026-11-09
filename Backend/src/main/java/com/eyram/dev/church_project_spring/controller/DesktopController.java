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
    public DesktopController(DesktopOperationService operations) { this.operations=operations; }
    @PostMapping("/operations")
    @PreAuthorize("isAuthenticated()")
    public JsonNode execute(@Valid @RequestBody DesktopOperationService.Operation operation) throws Exception {
        return operations.execute(operation);
    }
}
