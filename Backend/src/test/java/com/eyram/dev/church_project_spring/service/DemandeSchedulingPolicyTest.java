package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DemandeSchedulingPolicyTest {

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-07-16T11:45:00Z"),
            ZoneId.of("Africa/Lome")
    );
    private final DemandeSchedulingPolicy policy = new DemandeSchedulingPolicy(clock);

    @Test
    void rejectsPastTimeOnCurrentDay() {
        assertThrows(
                BusinessRuleException.class,
                () -> policy.validate(LocalDate.of(2026, 7, 16), LocalTime.of(6, 0), 0)
        );
    }

    @Test
    void rejectsCelebrationBeforeConfiguredLeadTime() {
        assertThrows(
                BusinessRuleException.class,
                () -> policy.validate(LocalDate.of(2026, 7, 17), LocalTime.of(10, 0), 24)
        );
    }

    @Test
    void acceptsCelebrationAfterConfiguredLeadTime() {
        assertDoesNotThrow(
                () -> policy.validate(LocalDate.of(2026, 7, 17), LocalTime.of(12, 0), 24)
        );
    }
}
