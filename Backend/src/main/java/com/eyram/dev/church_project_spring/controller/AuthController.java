package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.security.AuthService;
import com.eyram.dev.church_project_spring.security.dto.JwtResponse;
import com.eyram.dev.church_project_spring.security.dto.LoginRequest;
import com.eyram.dev.church_project_spring.security.jwt.AuthCookieService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService authCookieService;

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse httpResponse
    ) {
        JwtResponse jwt = authService.login(request);
        if (jwt.accessToken() != null && !jwt.accessToken().isBlank()) {
            authCookieService.writeAccessCookie(httpResponse, jwt.accessToken());
        }
        // Masque le jeton dans le JSON (cookie HttpOnly uniquement).
        return ResponseEntity.ok(new JwtResponse(
                null,
                jwt.tokenType(),
                jwt.publicId(),
                jwt.fullName(),
                jwt.username(),
                jwt.tenantId(),
                jwt.isGlobal(),
                jwt.roles(),
                jwt.permissions()
        ));
    }
}
