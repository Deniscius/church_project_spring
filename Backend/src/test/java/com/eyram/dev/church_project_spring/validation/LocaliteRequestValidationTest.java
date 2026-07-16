package com.eyram.dev.church_project_spring.validation;

import com.eyram.dev.church_project_spring.DTO.request.LocaliteRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LocaliteRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void acceptsValidLocality() {
        Set<ConstraintViolation<LocaliteRequest>> violations =
                validator.validate(new LocaliteRequest("Lomé", "Tokoin"));

        assertEquals(0, violations.size());
    }

    @Test
    void rejectsBlankFields() {
        Set<ConstraintViolation<LocaliteRequest>> violations =
                validator.validate(new LocaliteRequest(" ", ""));

        assertFalse(violations.isEmpty());
    }

    @Test
    void rejectsFieldsShorterThanTwoCharacters() {
        Set<ConstraintViolation<LocaliteRequest>> violations =
                validator.validate(new LocaliteRequest("L", "T"));

        assertEquals(2, violations.size());
    }
}
