package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseAssignmentRequest;
import com.eyram.dev.church_project_spring.DTO.request.UserRequest;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseAccessResponse;
import com.eyram.dev.church_project_spring.DTO.response.UserResponse;
import com.eyram.dev.church_project_spring.service.EnhancedUserService;
import com.eyram.dev.church_project_spring.utils.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.util.List;
import java.util.UUID;

/** Gestion des utilisateurs dans le périmètre contrôlé par le service métier. */
@Slf4j
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('user:manage')")
public class EnhancedUserController {

    private final EnhancedUserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Validated({Default.class, UserRequest.Create.class}) @RequestBody UserRequest request
    ) {
        log.info("POST /admin/users - Creating new user: {}", request.username());
        UUID createdBy = SecurityUtils.getCurrentUserPublicId();
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request, createdBy));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID userId,
            @Validated(Default.class) @RequestBody UserRequest request
    ) {
        UUID updatedBy = SecurityUtils.getCurrentUserPublicId();
        return ResponseEntity.ok(userService.updateUser(userId, request, updatedBy));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID userId) {
        UUID requestedBy = SecurityUtils.getCurrentUserPublicId();
        return ResponseEntity.ok(userService.getUserByPublicId(userId, requestedBy));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        UUID requestedBy = SecurityUtils.getCurrentUserPublicId();
        return ResponseEntity.ok(userService.getAllActiveUsers(requestedBy));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        UUID deletedBy = SecurityUtils.getCurrentUserPublicId();
        userService.deleteUser(userId, deletedBy);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/paroisses")
    public ResponseEntity<Void> assignParoisseToUser(
            @PathVariable UUID userId,
            @Valid @RequestBody ParoisseAssignmentRequest assignment
    ) {
        UUID assignedBy = SecurityUtils.getCurrentUserPublicId();
        userService.assignParoisseToUser(userId, assignment, assignedBy);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @DeleteMapping("/{userId}/paroisses/{paroisseId}")
    public ResponseEntity<Void> revokeParoisseAccess(
            @PathVariable UUID userId,
            @PathVariable UUID paroisseId
    ) {
        UUID revokedBy = SecurityUtils.getCurrentUserPublicId();
        userService.revokeParoisseAccess(userId, paroisseId, revokedBy);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/paroisses")
    public ResponseEntity<List<ParoisseAccessResponse>> getUserParoisses(@PathVariable UUID userId) {
        UUID requestedBy = SecurityUtils.getCurrentUserPublicId();
        return ResponseEntity.ok(userService.getUserParoisses(userId, requestedBy));
    }

    @GetMapping("/paroisse/{paroisseId}")
    public ResponseEntity<List<UserResponse>> getUsersByParoisse(@PathVariable UUID paroisseId) {
        UUID requestedBy = SecurityUtils.getCurrentUserPublicId();
        return ResponseEntity.ok(userService.getUsersByParoisse(paroisseId, requestedBy));
    }
}
