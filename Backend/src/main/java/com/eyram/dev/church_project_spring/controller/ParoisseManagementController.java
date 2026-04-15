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

import com.eyram.dev.church_project_spring.DTO.request.UserRequest;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseResponse;
import com.eyram.dev.church_project_spring.DTO.response.UserResponse;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.mappers.ParoisseMapper;
import com.eyram.dev.church_project_spring.service.ParoisseManagementService;
import com.eyram.dev.church_project_spring.utils.SecurityUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Contrôleur de gestion des paroisses.
 * Opérations réservées au SUPER_ADMIN et aux ADMIN locaux.
 */
@Slf4j
@RestController
@RequestMapping("/admin/paroisses")
@RequiredArgsConstructor
public class ParoisseManagementController {

    private final ParoisseManagementService paroisseManagementService;
    private final ParoisseMapper paroisseMapper;

    /**
     * Crée une nouvelle paroisse.
     * Accès: SUPER_ADMIN uniquement
     */
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ParoisseResponse> createParoisse(@Valid @RequestBody Paroisse paroisse) {
        log.info("POST /admin/paroisses - Creating new paroisse");
        UUID createdBy = SecurityUtils.getCurrentUserPublicId();
        Paroisse created = paroisseManagementService.createParoisse(paroisse, createdBy);
        return new ResponseEntity<>(paroisseMapper.modelToDto(created), HttpStatus.CREATED);
    }

    /**
     * Met à jour une paroisse.
     * Accès: SUPER_ADMIN ou ADMIN local de la paroisse
     */
    @PutMapping("/{paroisseId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ParoisseResponse> updateParoisse(
            @PathVariable Long paroisseId,
            @Valid @RequestBody Paroisse paroisse) {
        log.info("PUT /admin/paroisses/{} - Updating paroisse", paroisseId);
        UUID updatedBy = SecurityUtils.getCurrentUserPublicId();
        Paroisse updated = paroisseManagementService.updateParoisse(paroisseId, paroisse, updatedBy);
        return ResponseEntity.ok(paroisseMapper.modelToDto(updated));
    }

    /**
     * Désactive une paroisse.
     * Accès: SUPER_ADMIN uniquement
     */
    @DeleteMapping("/{paroisseId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deactivateParoisse(@PathVariable Long paroisseId) {
        log.info("DELETE /admin/paroisses/{} - Deactivating paroisse", paroisseId);
        UUID deactivatedBy = SecurityUtils.getCurrentUserPublicId();
        paroisseManagementService.deactivateParoisse(paroisseId, deactivatedBy);
        return ResponseEntity.noContent().build();
    }

    /**
     * Assigne un administrateur local à une paroisse.
     * Crée automatiquement un utilisateur ADMIN et l'assigne à la paroisse.
     * Accès: SUPER_ADMIN uniquement
     */
    @PostMapping("/{paroisseId}/assign-admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserResponse> assignAdminToParoisse(
            @PathVariable Long paroisseId,
            @Valid @RequestBody UserRequest userRequest) {
        log.info("POST /admin/paroisses/{}/assign-admin - Assigning admin", paroisseId);
        UUID assignedBy = SecurityUtils.getCurrentUserPublicId();
        UserResponse assigned = paroisseManagementService.assignAdminToParoisse(paroisseId, userRequest, assignedBy);
        return new ResponseEntity<>(assigned, HttpStatus.CREATED);
    }

    /**
     * Récupère tous les administrateurs d'une paroisse.
     * Accès: SUPER_ADMIN ou ADMIN de la paroisse
     */
    @GetMapping("/{paroisseId}/admins")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<UserResponse>> getParoisseAdmins(@PathVariable Long paroisseId) {
        log.info("GET /admin/paroisses/{}/admins - Getting admins", paroisseId);
        List<UserResponse> admins = paroisseManagementService.getParoisseAdmins(paroisseId);
        return ResponseEntity.ok(admins);
    }

    /**
     * Récupère toutes les paroisses actives.
     * Accès: Lecture publique (tous authentifiés)
     */
    @GetMapping
    public ResponseEntity<List<ParoisseResponse>> getAllParoisses() {
        log.info("GET /admin/paroisses - Getting all paroisses");
        List<Paroisse> paroisses = paroisseManagementService.getAllActiveParoisses();
        List<ParoisseResponse> responses = paroisses.stream()
            .map(paroisseMapper::modelToDto)
            .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * Récupère une paroisse spécifique.
     */
    @GetMapping("/{paroisseId}")
    public ResponseEntity<ParoisseResponse> getParoisse(@PathVariable Long paroisseId) {
        log.info("GET /admin/paroisses/{} - Getting paroisse", paroisseId);
        Paroisse paroisse = paroisseManagementService.getParoisseById(paroisseId);
        return ResponseEntity.ok(paroisseMapper.modelToDto(paroisse));
    }

    /**
     * Vérifie les statistiques des paroisses.
     * Accès: SUPER_ADMIN
     */
    @GetMapping("/stats/count")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Long> getParoisseCount() {
        log.info("GET /admin/paroisses/stats/count - Getting paroisse count");
        long count = paroisseManagementService.getActiveParoisseCount();
        return ResponseEntity.ok(count);
    }
}
