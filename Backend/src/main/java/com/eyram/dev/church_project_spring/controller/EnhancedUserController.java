package com.eyram.dev.church_project_spring.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseAssignmentRequest;
import com.eyram.dev.church_project_spring.DTO.request.UserRequest;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseAccessResponse;
import com.eyram.dev.church_project_spring.DTO.response.UserResponse;
import com.eyram.dev.church_project_spring.mappers.ParoisseAccessMapper;
import com.eyram.dev.church_project_spring.service.EnhancedUserService;
import com.eyram.dev.church_project_spring.utils.SecurityUtils;

import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Contrôleur de gestion des utilisateurs avec support multi-tenant.
 */
@Slf4j
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class EnhancedUserController {

    private final EnhancedUserService userService;
    private final ParoisseAccessMapper paroisseAccessMapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<UserResponse> createUser(
            @Validated({Default.class, UserRequest.Create.class})
            @RequestBody UserRequest request
    ) {
        log.info("POST /admin/users - Creating new user: {}", request.username());
        UUID createdBy = SecurityUtils.getCurrentUserPublicId();
        UserResponse created = userService.createUser(request, createdBy);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID userId,
            @Validated(Default.class) @RequestBody UserRequest request
    ) {
        log.info("PUT /admin/users/{} - Updating user", userId);
        UUID updatedBy = SecurityUtils.getCurrentUserPublicId();
        UserResponse updated = userService.updateUser(userId, request, updatedBy);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID userId) {
        log.info("GET /admin/users/{} - Getting user", userId);
        UUID requestedBy = SecurityUtils.getCurrentUserPublicId();
        UserResponse user = userService.getUserByPublicId(userId, requestedBy);
        return ResponseEntity.ok(user);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("GET /admin/users - Getting users in authorized scope");
        UUID requestedBy = SecurityUtils.getCurrentUserPublicId();
        List<UserResponse> users = userService.getAllActiveUsers(requestedBy);
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        log.info("DELETE /admin/users/{} - Deleting user", userId);
        UUID deletedBy = SecurityUtils.getCurrentUserPublicId();
        userService.deleteUser(userId, deletedBy);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/paroisses")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> assignParoisseToUser(
            @PathVariable UUID userId,
            @Valid @RequestBody ParoisseAssignmentRequest assignment
    ) {
        log.info("POST /admin/users/{}/paroisses - Assigning paroisse", userId);
        UUID assignedBy = SecurityUtils.getCurrentUserPublicId();
        userService.assignParoisseToUser(userId, assignment, assignedBy);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @DeleteMapping("/{userId}/paroisses/{paroisseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> revokeParoisseAccess(
            @PathVariable UUID userId,
            @PathVariable UUID paroisseId
    ) {
        log.info("DELETE /admin/users/{}/paroisses/{} - Revoking paroisse access", userId, paroisseId);
        UUID revokedBy = SecurityUtils.getCurrentUserPublicId();
        userService.revokeParoisseAccess(userId, paroisseId, revokedBy);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/paroisses")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<ParoisseAccessResponse>> getUserParoisses(@PathVariable UUID userId) {
        log.info("GET /admin/users/{}/paroisses - Getting user paroisses", userId);
        UUID requestedBy = SecurityUtils.getCurrentUserPublicId();
        List<ParoisseAccessResponse> paroisses = userService
                .getUserParoisses(userId, requestedBy)
                .stream()
                .map(paroisseAccessMapper::modelToDto)
                .toList();
        return ResponseEntity.ok(paroisses);
    }

    @GetMapping("/paroisse/{paroisseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<UserResponse>> getUsersByParoisse(@PathVariable UUID paroisseId) {
        log.info("GET /admin/users/paroisse/{} - Getting users by paroisse", paroisseId);
        UUID requestedBy = SecurityUtils.getCurrentUserPublicId();
        List<UserResponse> users = userService.getUsersByParoisse(paroisseId, requestedBy);
        return ResponseEntity.ok(users);
    }
}
