package com.eyram.dev.church_project_spring.utils.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception levée lorsqu'une règle métier est violée.
 * 
 * Utilisée pour:
 * - Validations métier (unicité, dépendances, etc.)
 * - Conflits de données (duplicatas, états invalides, etc.)
 * - Violations de contraintes applicatives
 * 
 * Status HTTP: 409 Conflict (car la requête entre en conflit avec l'état actuel)
 */
@ResponseStatus(value = HttpStatus.CONFLICT)
public class BusinessRuleException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public BusinessRuleException(String message) {
        super(message);
    }

    public BusinessRuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
