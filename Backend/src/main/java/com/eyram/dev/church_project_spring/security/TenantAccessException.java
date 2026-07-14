package com.eyram.dev.church_project_spring.security;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class TenantAccessException extends UsernameNotFoundException {

    public TenantAccessException(String message) {
        super(message);
    }
}