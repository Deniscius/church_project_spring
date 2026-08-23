package com.eyram.dev.church_project_spring.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertMatches;

class BusinessCodeGeneratorTest {

    @Test
    void generatedTrackingCodeUsesFiftyBitToken() {
        String code = BusinessCodeGenerator.demandeCode("Saint Joseph");

        assertMatches("^MS-SJ-[A-HJ-NP-Z2-9]{10}$", code);
    }

    @Test
    void normalizesNewTrackingCodeWithOrWithoutSeparators() {
        assertEquals(
                "MS-SJ-K7M2XQ8W4P",
                BusinessCodeGenerator.normalizeDemandeTrackingCode("ms sj k7m2xq8w4p")
        );
    }

    @Test
    void keepsCompatibilityWithHistoricalSixCharacterCodes() {
        assertEquals(
                "MS-SJ-K7M2XQ",
                BusinessCodeGenerator.normalizeDemandeTrackingCode("mssjk7m2xq")
        );
        assertEquals(
                "MS-K7M2XQ",
                BusinessCodeGenerator.normalizeDemandeTrackingCode("k7m2xq")
        );
    }
}
