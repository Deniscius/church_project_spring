package com.eyram.dev.church_project_spring.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.eyram.dev.church_project_spring.DTO.response.UserResponse;
import com.eyram.dev.church_project_spring.entities.ParoisseAccess;
import com.eyram.dev.church_project_spring.service.EnhancedUserService;
import com.eyram.dev.church_project_spring.utils.SecurityUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Contrôleur de gestion des utilisateurs avec support multi-tenant.
 * 
 * Endpoitns:
 * - POST   /users                          (créer utilisateur)
 * - PUT    /users/{userId}                 (modifier utilisateur)
 * - GET    /users/{userId}                 (consulter utilisateur)
 * - POST   /users/{userId}/paroisses       (assigner paroisse)
 * - DELETE /users/{userId}/paroisses/{pid} (révoquer accès paroisse)
 * - GET    /users/{userId}/paroisses       (lister paroisses de l'utilisateur)
 */
@Slf4j
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class EnhancedUserController {

    private final EnhancedUserService userService;

    /**
     * Crée un nouvel utilisateur.
     * Accès: ADMIN ou SUPER_ADMIN
     *
     * Request:
     * {
     *   "nom": "Dupont",
     *   "prenom": "Jean",
     *   "username": "jean.dupont",
     *   "password": "SecurePass123!",
     *   "role": "SECRETAIRE",
     *   "isGlobal": false,
     *   "isActive": true,
     *   "paroisses": [
     *     { "paroisseId": "...", "roleParoisse": "SECRETAIRE" }
     *   ]
     * }
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        log.info("POST /admin/users - Creating new user: {}", request.username());
        UUID createdBy = SecurityUtils.getCurrentUserPublicId();
        UserResponse created = userService.createUser(request, createdBy);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Met à jour un utilisateur existant.
     * Accès: ADMIN (de sa paroisse) ou SUPER_ADMIN
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UserRequest request) {
        log.info("PUT /admin/users/{} - Updating user", userId);
        UUID updatedBy = SecurityUtils.getCurrentUserPublicId();
        UserResponse updated = userService.updateUser(userId, request, updatedBy);
        return ResponseEntity.ok(updated);
    }

    /**
     * Récupère un utilisateur spécifique.
     * Accès: L'utilisateur lui-même ou ADMIN/SUPER_ADMIN
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID userId) {
        log.info("GET /admin/users/{} - Getting user", userId);
        UserResponse user = userService.getUserByPublicId(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * Récupère tous les utilisateurs actifs.
     * Accès: ADMIN ou SUPER_ADMIN
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("GET /admin/users - Getting all users");
        List<UserResponse> users = userService.getAllActiveUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Désactive (soft delete) un utilisateur.
     * Accès: ADMIN de la paroisse ou SUPER_ADMIN
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        log.info("DELETE /admin/users/{} - Deleting user", userId);
        UUID deletedBy = SecurityUtils.getCurrentUserPublicId();
        userService.deleteUser(userId, deletedBy);
        return ResponseEntity.noContent().build();
    }

    /**
     * Assigne une paroisse à un utilisateur.
     * 
     * Request:
     * {
     *   "paroisseId": "...",
     *   "roleParoisse": "SECRETAIRE"
     * }
     *
     * Accès: ADMIN de cette paroisse ou SUPER_ADMIN
     */
    @PostMapping("/{userId}/paroisses")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> assignParoisseToUser(
            @PathVariable UUID userId,
            @Valid @RequestBody ParoisseAssignmentRequest assignment) {
        log.info("POST /admin/users/{}/paroisses - Assigning paroisse", userId);
        UUID assignedBy = SecurityUtils.getCurrentUserPublicId();
        userService.assignParoisseToUser(userId, assignment, assignedBy);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * Révoque l'accès d'un utilisateur à une paroisse.
     * Accès: ADMIN de cette paroisse ou SUPER_ADMIN
     */
    @DeleteMapping("/{userId}/paroisses/{paroisseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> revokeParoisseAccess(
            @PathVariable UUID userId,
            @PathVariable Long paroisseId) {
        log.info("DELETE /admin/users/{}/paroisses/{} - Revoking paroisse access", userId, paroisseId);
        UUID revokedBy = SecurityUtils.getCurrentUserPublicId();
        userService.revokeParoisseAccess(userId, paroisseId, revokedBy);
        return ResponseEntity.noContent().build();
    }

    /**
     * Récupère les paroisses d'un utilisateur.
     * Accès: L'utilisateur lui-même ou ADMIN/SUPER_ADMIN
     */
    @GetMapping("/{userId}/paroisses")
    public ResponseEntity<List<ParoisseAccess>> getUserParoisses(@PathVariable UUID userId) {
        log.info("GET /admin/users/{}/paroisses - Getting user paroisses", userId);
        List<ParoisseAccess> paroisses = userService.getUserParoisses(userId);
        return ResponseEntity.ok(paroisses);
    }

    /**
     * Récupère les utilisateurs d'une paroisse.
     * Accès: ADMIN de cette paroisse ou SUPER_ADMIN
     */
    @GetMapping("/paroisse/{paroisseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<UserResponse>> getUsersByParoisse(@PathVariable Long paroisseId) {
        log.info("GET /admin/users/paroisse/{} - Getting users by paroisse", paroisseId);
        List<UserResponse> users = userService.getUsersByParoisse(paroisseId);
        return ResponseEntity.ok(users);
    }
}
