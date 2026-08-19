package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.UserRequest;
import com.eyram.dev.church_project_spring.DTO.response.UserResponse;
import com.eyram.dev.church_project_spring.service.ParoisseManagementService;
import com.eyram.dev.church_project_spring.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/paroisses")
@RequiredArgsConstructor
@PreAuthorize("principal.isGlobal()")
public class ParoisseManagementController {

    private final ParoisseManagementService paroisseManagementService;

    @PostMapping("/{paroissePublicId}/assign-admin")
    @PreAuthorize("hasAuthority('parish-access:manage') and principal.isGlobal()")
    public ResponseEntity<UserResponse> assignAdminToParoisse(
            @PathVariable UUID paroissePublicId,
            @Valid @RequestBody UserRequest userRequest
    ) {
        UUID assignedBy = SecurityUtils.getCurrentUserPublicId();
        UserResponse assigned = paroisseManagementService.assignAdminToParoisse(
                paroissePublicId,
                userRequest,
                assignedBy
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(assigned);
    }

    @GetMapping("/{paroissePublicId}/admins")
    @PreAuthorize("hasAuthority('parish-access:manage') and principal.isGlobal()")
    public ResponseEntity<List<UserResponse>> getParoisseAdmins(
            @PathVariable UUID paroissePublicId
    ) {
        return ResponseEntity.ok(paroisseManagementService.getParoisseAdmins(paroissePublicId));
    }

    @GetMapping("/stats/count")
    @PreAuthorize("hasAuthority('parish:read') and principal.isGlobal()")
    public ResponseEntity<Long> getParoisseCount() {
        return ResponseEntity.ok(paroisseManagementService.getActiveParoisseCount());
    }
}
