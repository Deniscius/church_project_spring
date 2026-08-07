package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.ChangePasswordRequest;
import com.eyram.dev.church_project_spring.DTO.request.ProfileUpdateRequest;
import com.eyram.dev.church_project_spring.DTO.response.UserResponse;
import com.eyram.dev.church_project_spring.service.EnhancedUserService;
import com.eyram.dev.church_project_spring.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Libre-service du compte connecté. L'identifiant cible n'est jamais reçu du
 * client : il provient du contexte de sécurité, ce qui garantit qu'un
 * utilisateur ne peut modifier que ses propres informations.
 */
@Slf4j
@RestController
@RequestMapping("/admin/profile")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ProfileController {

    private final EnhancedUserService userService;

    @GetMapping
    public ResponseEntity<UserResponse> me() {
        return ResponseEntity.ok(
                userService.getUserByPublicId(SecurityUtils.getCurrentUserPublicId())
        );
    }

    @PutMapping
    public ResponseEntity<UserResponse> updateMe(@Valid @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(
                userService.updateOwnProfile(SecurityUtils.getCurrentUserPublicId(), request)
        );
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changeMyPassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changeOwnPassword(SecurityUtils.getCurrentUserPublicId(), request);
        return ResponseEntity.noContent().build();
    }
}
