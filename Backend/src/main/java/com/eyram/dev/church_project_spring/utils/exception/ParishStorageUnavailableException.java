package com.eyram.dev.church_project_spring.utils.exception;

/** A deployment/placement error must remain retryable by desktop clients. */
public class ParishStorageUnavailableException extends RuntimeException {
    public ParishStorageUnavailableException() {
        super("L'espace paroissial est temporairement indisponible. Votre travail est conservé.");
    }
}
