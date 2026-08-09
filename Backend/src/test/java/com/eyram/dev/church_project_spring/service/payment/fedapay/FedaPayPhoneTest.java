package com.eyram.dev.church_project_spring.service.payment.fedapay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class FedaPayPhoneTest {

    @Test
    void parsesE164Togo() {
        FedaPayPhone.Parts p = FedaPayPhone.parse("+22890123456", "tg");
        assertNotNull(p);
        assertEquals("90123456", p.number());
        assertEquals("tg", p.country());
    }

    @Test
    void stripsLeadingZeroNational() {
        FedaPayPhone.Parts p = FedaPayPhone.parse("090123456", "tg");
        assertNotNull(p);
        assertEquals("90123456", p.number());
        assertEquals("tg", p.country());
    }

    @Test
    void detectsBeninFromE164() {
        FedaPayPhone.Parts p = FedaPayPhone.parse("+22966000001", "tg");
        assertNotNull(p);
        assertEquals("66000001", p.number());
        assertEquals("bj", p.country());
    }

    @Test
    void rejectsTooShort() {
        assertNull(FedaPayPhone.parse("+22890", "tg"));
        assertNull(FedaPayPhone.toCustomerPhone("abc", "tg"));
    }
}
