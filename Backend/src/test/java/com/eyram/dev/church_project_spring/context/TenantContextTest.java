package com.eyram.dev.church_project_spring.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TenantContextTest {

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void withoutTenantClearsAndRestoresExistingTenant() {
        TenantContext.setCurrentTenant(42L);

        Long valueSeenInside = TenantContext.withoutTenant(TenantContext::getCurrentTenant);

        assertNull(valueSeenInside);
        assertEquals(42L, TenantContext.getCurrentTenant());
    }

    @Test
    void withoutTenantRestoresContextWhenActionFails() {
        TenantContext.setCurrentTenant(7L);

        assertThrows(IllegalStateException.class, () ->
                TenantContext.withoutTenant(() -> {
                    assertNull(TenantContext.getCurrentTenant());
                    throw new IllegalStateException("expected");
                })
        );

        assertEquals(7L, TenantContext.getCurrentTenant());
    }

    @Test
    void withoutTenantKeepsEmptyContextEmpty() {
        String result = TenantContext.withoutTenant(() -> "ok");

        assertEquals("ok", result);
        assertNull(TenantContext.getCurrentTenant());
    }
}
