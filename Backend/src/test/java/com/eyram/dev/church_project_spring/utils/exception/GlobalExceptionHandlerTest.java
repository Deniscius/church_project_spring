package com.eyram.dev.church_project_spring.utils.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("GlobalExceptionHandler tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private WebRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setMethod("POST");
        servletRequest.setRequestURI("/test");
        request = new ServletWebRequest(servletRequest);
    }

    @Test
    @DisplayName("Business rule violations return 409 Conflict")
    void businessRuleExceptionReturnsConflict() {
        ResponseEntity<ErrorMessage> response = handler.handleConflict(
                new BusinessRuleException("Règle métier non respectée"),
                request
        );

        assertResponse(response, HttpStatus.CONFLICT, "Règle métier non respectée");
    }

    @Test
    @DisplayName("Duplicate resources return 409 Conflict")
    void alreadyExistExceptionReturnsConflict() {
        ResponseEntity<ErrorMessage> response = handler.handleConflict(
                new AlreadyExistException("La ressource existe déjà"),
                request
        );

        assertResponse(response, HttpStatus.CONFLICT, "La ressource existe déjà");
    }

    @Test
    @DisplayName("Missing entities return 404 Not Found")
    void entityNotFoundExceptionReturnsNotFound() {
        ResponseEntity<ErrorMessage> response = handler.handleNotFound(
                new EntityNotFoundException("Paroisse non trouvée"),
                request
        );

        assertResponse(response, HttpStatus.NOT_FOUND, "Paroisse non trouvée");
    }

    @Test
    @DisplayName("Malformed JSON returns a safe 400 response")
    void malformedBodyReturnsBadRequest() {
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "Technical parser details",
                new MockHttpInputMessage(new byte[0])
        );

        ResponseEntity<ErrorMessage> response =
                handler.handleHttpMessageNotReadableException(exception, request);

        assertResponse(
                response,
                HttpStatus.BAD_REQUEST,
                "Le corps de la requête est absent ou invalide"
        );
    }

    @Test
    @DisplayName("Database constraint violations return a safe 409 response")
    void dataIntegrityViolationReturnsConflict() {
        ResponseEntity<ErrorMessage> response = handler.handleDataIntegrityViolationException(
                new DataIntegrityViolationException("Sensitive SQL constraint details"),
                request
        );

        assertResponse(
                response,
                HttpStatus.CONFLICT,
                "L'opération entre en conflit avec les données existantes"
        );
    }

    @Test
    @DisplayName("Unexpected failures do not expose internal exception messages")
    void unexpectedExceptionReturnsSafeInternalError() {
        String sensitiveMessage = "database password=secret";

        ResponseEntity<ErrorMessage> response = handler.handleAllException(
                new RuntimeException(sensitiveMessage),
                request
        );

        assertNotNull(response.getBody());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().getStatusCode());
        assertEquals(
                "Une erreur interne est survenue. Veuillez réessayer plus tard.",
                response.getBody().getMessage()
        );
        assertNotEquals(sensitiveMessage, response.getBody().getMessage());
    }

    private void assertResponse(
            ResponseEntity<ErrorMessage> response,
            HttpStatus expectedStatus,
            String expectedMessage
    ) {
        assertNotNull(response.getBody());
        assertEquals(expectedStatus, response.getStatusCode());
        assertEquals(expectedStatus.value(), response.getBody().getStatusCode());
        assertEquals(expectedMessage, response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
    }
}
