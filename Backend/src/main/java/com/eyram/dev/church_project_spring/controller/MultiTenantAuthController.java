package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.security.MultiTenantAuthService;
import com.eyram.dev.church_project_spring.security.UserDetailsImpl;
import com.eyram.dev.church_project_spring.security.dto.LoginRequest;
import com.eyram.dev.church_project_spring.security.dto.MultiTenantLoginResponse;
import com.eyram.dev.church_project_spring.security.jwt.AuthCookieService;
import com.eyram.dev.church_project_spring.service.auth.PasswordResetService;
import com.eyram.dev.church_project_spring.DTO.request.ForgotPasswordRequest;
import com.eyram.dev.church_project_spring.DTO.request.ResetPasswordRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Authentification multi-tenant + réinitialisation de mot de passe.
 * Le JWT est livré uniquement via cookie HttpOnly chiffré (jamais dans le JSON).
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class MultiTenantAuthController {

    private final MultiTenantAuthService multiTenantAuthService;
    private final PasswordResetService passwordResetService;
    private final AuthCookieService authCookieService;

    @PostMapping("/login-multi-tenant")
    public ResponseEntity<MultiTenantLoginResponse> loginMultiTenant(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletResponse httpResponse
    ) {
        MultiTenantLoginResponse response = multiTenantAuthService.loginMultiTenant(loginRequest);
        String token = response.getToken();
        if (token != null && !token.isBlank()) {
            authCookieService.writeAccessCookie(httpResponse, token);
        }
        // Ne jamais exposer le JWT dans le corps JSON (DevTools / XSS).
        response.setToken(null);
        return ResponseEntity.ok(response);
    }

    /** Session courante (cookie) — sans renvoyer le jeton. */
    @GetMapping("/me")
    public ResponseEntity<MultiTenantLoginResponse> me(@AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(multiTenantAuthService.currentSession(principal));
    }

    /** Invalide le cookie d'accès côté navigateur. */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletResponse httpResponse) {
        authCookieService.clearAccessCookie(httpResponse);
        return ResponseEntity.ok(Map.of("message", "Déconnexion effectuée"));
    }

    /** Toujours 200 + message générique (anti-énumération). */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest body,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(passwordResetService.requestReset(body, clientIp(httpRequest)));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest body) {
        return ResponseEntity.ok(passwordResetService.resetPassword(body));
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
