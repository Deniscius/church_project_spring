package com.eyram.dev.church_project_spring.validation;

import com.eyram.dev.church_project_spring.DTO.request.UserRequest;
import com.eyram.dev.church_project_spring.enums.UserRole;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.groups.Default;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("UserRequest password validation tests")
class UserRequestValidationTest {

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
    @DisplayName("Creation requires a non-blank password")
    void createRejectsBlankPassword() {
        Set<ConstraintViolation<UserRequest>> violations = validator.validate(
                request(""),
                Default.class,
                UserRequest.Create.class
        );

        assertTrue(hasPasswordViolation(violations));
    }

    @Test
    @DisplayName("Update accepts a blank password to preserve the current one")
    void updateAcceptsBlankPassword() {
        Set<ConstraintViolation<UserRequest>> violations = validator.validate(
                request(""),
                Default.class
        );

        assertEquals(0, violations.size());
    }

    @Test
    @DisplayName("Update rejects a provided password shorter than eight characters")
    void updateRejectsShortPassword() {
        Set<ConstraintViolation<UserRequest>> violations = validator.validate(
                request("short"),
                Default.class
        );

        assertTrue(hasPasswordViolation(violations));
    }

    private boolean hasPasswordViolation(Set<ConstraintViolation<UserRequest>> violations) {
        return violations.stream()
                .anyMatch(violation -> "password".equals(violation.getPropertyPath().toString()));
    }

    private UserRequest request(String password) {
        return new UserRequest(
                "Dupont",
                "Jean",
                "jean.dupont",
                password,
                true,
                true,
                UserRole.ADMIN,
                null
        );
    }
}
