package com.eyram.dev.church_project_spring.security;

import com.eyram.dev.church_project_spring.entities.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class UserDetailsImpl implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final UUID publicId;
    private final String fullName;
    private final String username;
    private final Long tenantId;
    private final boolean isGlobal;
    private final long tokenVersion;
    @JsonIgnore
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;

    public UserDetailsImpl(UUID publicId,
                           String fullName,
                           String username,
                           Long tenantId,
                           boolean isGlobal,
                           long tokenVersion,
                           String password,
                           Collection<? extends GrantedAuthority> authorities,
                           boolean enabled) {
        this.publicId = publicId;
        this.fullName = fullName;
        this.username = username;
        this.tenantId = tenantId;
        this.isGlobal = isGlobal;
        this.tokenVersion = tokenVersion;
        this.password = password;
        this.authorities = authorities;
        this.enabled = enabled;
    }

    public static UserDetailsImpl build(User user, Long tenantId) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        // Le rôle reste présent pour la compatibilité Spring/diagnostic.
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        // Les décisions métier utilisent les permissions granulaires.
        RolePermissions.authoritiesFor(user.getRole()).stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        return new UserDetailsImpl(
                user.getPublicId(),
                user.getFullName(),
                user.getUsername(),
                tenantId,
                Boolean.TRUE.equals(user.getIsGlobal()),
                user.getTokenVersion() == null ? 0L : user.getTokenVersion(),
                user.getPassword(),
                Set.copyOf(authorities),
                Boolean.TRUE.equals(user.getIsActive())
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserDetailsImpl that = (UserDetailsImpl) o;
        return Objects.equals(publicId, that.publicId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(publicId);
    }
}
