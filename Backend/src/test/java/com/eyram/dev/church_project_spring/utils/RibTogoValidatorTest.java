package com.eyram.dev.church_project_spring.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RibTogoValidatorTest {

    private static final String VALID_ECOBANK_IBAN = "TG53 TG00 9060 4310 3465 0040 0070";

    @Test
    void acceptsValidTogoIbanForMatchingBank() {
        var result = RibTogoValidator.validate(VALID_ECOBANK_IBAN, "Ecobank Togo");
        assertTrue(result.valid(), result.message());
        assertEquals("TG53TG0090604310346500400070", result.normalized());
        assertEquals("00906", result.bankCode());
    }

    @Test
    void rejectsIbanWhenBankDoesNotMatch() {
        var result = RibTogoValidator.validate(VALID_ECOBANK_IBAN, "Orabank Togo");
        assertFalse(result.valid());
        assertTrue(result.message().contains("00906"));
        assertTrue(result.message().contains("00904"));
    }

    @Test
    void acceptsDomesticBban() {
        var result = RibTogoValidator.validate("TG0090604310346500400070", "Ecobank Togo");
        assertTrue(result.valid(), result.message());
        assertEquals("00906", result.bankCode());
    }

    @Test
    void rejectsBadChecksum() {
        var result = RibTogoValidator.validate("TG00TG0090604310346500400070", "Ecobank Togo");
        assertFalse(result.valid());
        assertTrue(result.message().toLowerCase().contains("clé"));
    }
}
