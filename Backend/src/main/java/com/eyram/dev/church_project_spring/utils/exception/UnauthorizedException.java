package com.eyram.dev.church_project_spring.utils.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception levée lorsqu'un utilisateur n'a pas les permissions nécessaires.
 * 
 * Utilisée pour:
 * - Accès non autorisé à une ressource
 * - Permissions insuffisantes pour une opération
 * - Violations de contrôle d'accès
 * 
 * Status HTTP: 403 Forbidden
 */
@ResponseStatus(value = HttpStatus.FORBIDDEN)
public class UnauthorizedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
