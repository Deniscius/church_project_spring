package com.eyram.dev.church_project_spring.validation;

import com.eyram.dev.church_project_spring.DTO.request.DemandeRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("DemandeRequest validation tests")
class DemandeRequestValidationTest {

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
    @DisplayName("Blank mandatory fields, invalid email and missing date are rejected")
    void invalidRequestIsRejected() {
        DemandeRequest request = new DemandeRequest(
                "   ",
                "",
                "   ",
                "",
                "adresse-invalide",
                null,
                null,
                null,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                UUID.randomUUID()
        );

        Set<String> invalidFields = validator.validate(request)
                .stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertTrue(invalidFields.contains("intention"));
        assertTrue(invalidFields.contains("nomFidele"));
        assertTrue(invalidFields.contains("prenomFidele"));
        assertTrue(invalidFields.contains("telFidele"));
        assertTrue(invalidFields.contains("emailFidele"));
        assertTrue(invalidFields.contains("dateDebut"));
    }

    @Test
    @DisplayName("A well-formed request passes bean validation")
    void validRequestPassesValidation() {
        DemandeRequest request = new DemandeRequest(
                "Action de grâce",
                "KOFFI",
                "Eyram",
                "+22890000000",
                "eyram@example.com",
                "Coursier Test",
                null,
                LocalDate.now().plusDays(1),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                UUID.randomUUID()
        );

        Set<ConstraintViolation<DemandeRequest>> violations = validator.validate(request);

        assertEquals(0, violations.size());
    }
}
