package com.eyram.dev.church_project_spring.utils.exception;

import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String INTERNAL_ERROR_MESSAGE =
            "Une erreur interne est survenue. Veuillez réessayer plus tard.";

    @ExceptionHandler({AlreadyExistException.class, BusinessRuleException.class})
    public ResponseEntity<ErrorMessage> handleConflict(
            RuntimeException ex,
            WebRequest request
    ) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler({
            RequestNotFoundException.class,
            TrackingIdNotFoundException.class,
            ResourceNotFoundException.class,
            EntityNotFoundException.class
    })
    public ResponseEntity<ErrorMessage> handleNotFound(
            RuntimeException ex,
            WebRequest request
    ) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<ErrorMessage> handleAccountDisabled(
            AccountDisabledException ex,
            WebRequest request
    ) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorMessage> handleInvalidCredentials(
            InvalidCredentialsException ex,
            WebRequest request
    ) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorMessage> handleAccessDeniedException(
            AccessDeniedException ex,
            WebRequest request
    ) {
        return buildResponse(HttpStatus.FORBIDDEN, "Accès refusé", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorMessage> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request
    ) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorMessage> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            WebRequest request
    ) {
        String details = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    if (error instanceof FieldError fieldError) {
                        String fieldName = fieldError.getField();
                        String message = fieldError.getDefaultMessage();
                        if (fieldName == null || fieldName.isBlank()) {
                            fieldName = "champ";
                        }
                        if (message == null || message.isBlank()) {
                            message = "Valeur invalide";
                        }
                        return fieldName + ": " + message;
                    }
                    String defaultMessage = error.getDefaultMessage();
                    return defaultMessage != null && !defaultMessage.isBlank()
                            ? defaultMessage
                            : "Valeur invalide";
                })
                .collect(Collectors.joining(" ; "));

        return buildResponse(HttpStatus.BAD_REQUEST, details, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorMessage> handleConstraintViolationException(
            ConstraintViolationException ex,
            WebRequest request
    ) {
        String details = ex.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(" ; "));

        return buildResponse(HttpStatus.BAD_REQUEST, details, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorMessage> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex,
            WebRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Le corps de la requête est absent ou invalide",
                request
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorMessage> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            WebRequest request
    ) {
        String message = "Valeur invalide pour le paramètre '" + ex.getName() + "'";
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorMessage> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex,
            WebRequest request
    ) {
        log.warn("Database constraint violation on {}", request.getDescription(false), ex);
        return buildResponse(
                HttpStatus.CONFLICT,
                resolveDataIntegrityMessage(ex),
                request
        );
    }

    private String resolveDataIntegrityMessage(DataIntegrityViolationException ex) {
        String details = "";
        if (ex.getMostSpecificCause() != null && ex.getMostSpecificCause().getMessage() != null) {
            details = ex.getMostSpecificCause().getMessage().toLowerCase();
        } else if (ex.getMessage() != null) {
            details = ex.getMessage().toLowerCase();
        }

        if (details.contains("uq_forfait_tarif_type_nature")
                || details.contains("type_demande_id") && details.contains("nature_forfait")) {
            return "Un forfait avec cette nature existe déjà pour ce type de demande "
                    + "(normale, dominicale ou spéciale — une seule par nature, y compris les forfaits soft-supprimés).";
        }
        if (details.contains("forfait_tarif_jour_autorise")
                || details.contains("type_demande_jour_autorise")) {
            return "Impossible d'enregistrer les jours de célébration. Réessayez ou vérifiez qu'aucun doublon de jour n'est envoyé.";
        }
        if (details.contains("code_forfait")) {
            return "Ce code forfait existe déjà.";
        }
        if (details.contains("nom_forfait")) {
            return "Un forfait avec ce nom existe déjà pour ce type de demande.";
        }
        if (details.contains("libelle") && details.contains("paroisse")) {
            return "Ce type de demande existe déjà pour cette paroisse.";
        }

        return "L'opération entre en conflit avec les données existantes";
    }

    @ExceptionHandler(AuthenticationServiceException.class)
    public ResponseEntity<ErrorMessage> handleAuthenticationServiceException(
            AuthenticationServiceException ex,
            WebRequest request
    ) {
        String traceId = UUID.randomUUID().toString();
        log.error("Authentication service failure on {} [traceId={}]", request.getDescription(false), traceId, ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MESSAGE, request, traceId);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessage> handleAllException(
            Exception ex,
            WebRequest request
    ) {
        String traceId = UUID.randomUUID().toString();
        log.error("Unhandled exception on {} [traceId={}]", request.getDescription(false), traceId, ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MESSAGE, request, traceId);
    }

    private ResponseEntity<ErrorMessage> buildResponse(
            HttpStatus status,
            String message,
            WebRequest request
    ) {
        return buildResponse(status, message, request, UUID.randomUUID().toString());
    }

    private ResponseEntity<ErrorMessage> buildResponse(
            HttpStatus status,
            String message,
            WebRequest request,
            String traceId
    ) {
        String path = request.getDescription(false);
        if (path != null && path.startsWith("uri=")) {
            path = path.substring(4);
        }

        ErrorMessage response = new ErrorMessage(
                status.value(),
                new Date(),
                message,
                path,
                traceId
        );
        return ResponseEntity.status(status)
                .header("X-Request-Id", traceId)
                .body(response);
    }
}
