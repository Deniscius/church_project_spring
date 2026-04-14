package com.eyram.dev.church_project_spring.security;

import com.eyram.dev.church_project_spring.entities.User;
import com.eyram.dev.church_project_spring.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class UserDetailsImpl implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final UUID publicId;
    private final String fullName;
    private final String username;
    @JsonIgnore
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;

    public UserDetailsImpl(UUID publicId,
                           String fullName,
                           String username,
                           String password,
                           Collection<? extends GrantedAuthority> authorities,
                           boolean enabled) {
        this.publicId = publicId;
        this.fullName = fullName;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.enabled = enabled;
    }

    public static UserDetailsImpl build(User user) {
        UserRole role = user.getRole();
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + role.name())
        );
        return new UserDetailsImpl(
                user.getPublicId(),
                user.getFullName(),
                user.getUsername(),
                user.getPassword(),
                authorities,
                Boolean.TRUE.equals(user.getIsActive())
        );
    }

    public UUID getPublicId() {
        return publicId;
    }

    public String getFullName() {
        return fullName;
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
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserDetailsImpl that = (UserDetailsImpl) o;
        return Objects.equals(publicId, that.publicId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(publicId);
    }
}
