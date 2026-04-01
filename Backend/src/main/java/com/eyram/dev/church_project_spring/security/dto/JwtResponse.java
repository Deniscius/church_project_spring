package com.eyram.dev.church_project_spring.security.dto;

import com.eyram.dev.church_project_spring.security.UserDetailsImpl;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.UUID;

public record JwtResponse(
        String accessToken,
        String tokenType,
        UUID publicId,
        String fullName,
        String username,
        List<String> roles
) {
    public static JwtResponse from(String accessToken, UserDetailsImpl principal) {
        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return new JwtResponse(
                accessToken,
                "Bearer",
                principal.getPublicId(),
                principal.getFullName(),
                principal.getUsername(),
                roles
        );
    }
}
