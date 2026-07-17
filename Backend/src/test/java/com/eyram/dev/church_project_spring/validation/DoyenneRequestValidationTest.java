package com.eyram.dev.church_project_spring.validation;

import com.eyram.dev.church_project_spring.DTO.request.DoyenneRequest;
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

class DoyenneRequestValidationTest {

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
    void acceptsValidDeanery() {
        Set<ConstraintViolation<DoyenneRequest>> violations =
                validator.validate(new DoyenneRequest("Doyenné de Lomé-Centre", "Zone pastorale"));

        assertEquals(0, violations.size());
    }

    @Test
    void rejectsBlankName() {
        Set<ConstraintViolation<DoyenneRequest>> violations =
                validator.validate(new DoyenneRequest(" ", ""));

        assertFalse(violations.isEmpty());
    }

    @Test
    void rejectsNameShorterThanTwoCharacters() {
        Set<ConstraintViolation<DoyenneRequest>> violations =
                validator.validate(new DoyenneRequest("L", "T"));

        assertEquals(1, violations.size());
    }
}
