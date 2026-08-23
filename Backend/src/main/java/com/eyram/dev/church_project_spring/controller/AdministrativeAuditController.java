package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.response.AdministrativeAuditEventResponse;
import com.eyram.dev.church_project_spring.service.audit.AdministrativeAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/administrative-audit")
@RequiredArgsConstructor
public class AdministrativeAuditController {

    private final AdministrativeAuditService auditService;

    @GetMapping
    @PreAuthorize("hasAuthority('administrative-audit:read') and principal.isGlobal()")
    public ResponseEntity<List<AdministrativeAuditEventResponse>> listRecent() {
        return ResponseEntity.ok(auditService.listRecent());
    }
}
