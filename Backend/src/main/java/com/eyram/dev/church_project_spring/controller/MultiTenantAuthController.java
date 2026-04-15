package com.eyram.dev.church_project_spring.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eyram.dev.church_project_spring.security.MultiTenantAuthService;
import com.eyram.dev.church_project_spring.security.dto.LoginRequest;
import com.eyram.dev.church_project_spring.security.dto.MultiTenantLoginResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Contrôleur d'authentification multi-tenant.
 * Fournit un endpoint de connexion spécifiquement conçu pour les systèmes multi-paroisse.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class MultiTenantAuthController {

    private final MultiTenantAuthService multiTenantAuthService;

    /**
     * Connecte un utilisateur et retourne ses paroisses d'accès.
     *
     * @param loginRequest identifiants (username, password)
     * @return réponse contenant JWT, informations utilisateur et paroisses
     */
    @PostMapping("/login-multi-tenant")
    public ResponseEntity<MultiTenantLoginResponse> loginMultiTenant(@Valid @RequestBody LoginRequest loginRequest) {
        MultiTenantLoginResponse response = multiTenantAuthService.loginMultiTenant(loginRequest);
        return ResponseEntity.ok(response);
    }
}
