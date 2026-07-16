package com.eyram.dev.church_project_spring.validation;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ParoisseRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsAValidRequest() {
        ParoisseRequest request = new ParoisseRequest(
                "Saint Jean",
                "12 rue de la Paix",
                "contact@example.com",
                "+228 90 00 00 00",
                UUID.randomUUID()
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsMissingRequiredFieldsAndInvalidEmail() {
        ParoisseRequest request = new ParoisseRequest("", "", "incorrect", null, null);

        Set<ConstraintViolation<ParoisseRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("nom")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("adresse")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("localitePublicId")));
    }
}
