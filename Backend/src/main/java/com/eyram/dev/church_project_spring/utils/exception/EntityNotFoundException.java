package com.eyram.dev.church_project_spring.utils.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception levée lorsqu'une entité demandée n'existe pas.
 * 
 * Utilisée pour:
 * - Recherche d'entité par ID (User, Paroisse, etc.) introuvable
 * - Accès à une ressource supprimée ou inexistante
 * - Opérations sur des entités manquantes
 * 
 * Status HTTP: 404 Not Found
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class EntityNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
