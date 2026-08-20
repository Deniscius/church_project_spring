package com.eyram.dev.church_project_spring.security.dto;

import com.eyram.dev.church_project_spring.security.UserDetailsImpl;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record JwtResponse(
        String accessToken,
        String tokenType,
        UUID publicId,
        String fullName,
        String username,
        Long tenantId,
        boolean isGlobal,
        List<String> roles,
        List<String> permissions
) {

    public static JwtResponse from(String accessToken, UserDetailsImpl principal) {
        List<String> authorities = principal.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        List<String> roles = authorities.stream()
                .filter(authority -> authority.startsWith("ROLE_"))
                .toList();

        List<String> permissions = authorities.stream()
                .filter(authority -> !authority.startsWith("ROLE_"))
                .toList();

        return new JwtResponse(
                accessToken,
                "Bearer",
                principal.getPublicId(),
                principal.getFullName(),
                principal.getUsername(),
                principal.getTenantId(),
                principal.isGlobal(),
                roles,
                permissions
        );
    }
}
