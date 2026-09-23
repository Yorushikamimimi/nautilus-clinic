package com.ruoyi.clinic.util;

import com.ruoyi.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrescriptionUtilsTest {

    @Test
    void acceptsPositiveWholeQuantities() {
        assertEquals(1, PrescriptionUtils.parseQuantity(1));
        assertEquals(7, PrescriptionUtils.parseQuantity(new BigDecimal("7.0")));
        assertEquals(12, PrescriptionUtils.parseQuantity("12"));
    }

    @Test
    void rejectsMissingNonPositiveFractionalAndOverflowQuantities() {
        assertThrows(ServiceException.class, () -> PrescriptionUtils.parseQuantity(null));
        assertThrows(ServiceException.class, () -> PrescriptionUtils.parseQuantity(0));
        assertThrows(ServiceException.class, () -> PrescriptionUtils.parseQuantity(-1));
        assertThrows(ServiceException.class, () -> PrescriptionUtils.parseQuantity(new BigDecimal("1.5")));
        assertThrows(ServiceException.class, () -> PrescriptionUtils.parseQuantity("2147483648"));
    }

    @Test
    void rejectsMissingOrBlankItemCodes() {
        assertThrows(ServiceException.class, () -> PrescriptionUtils.requireItemCode(null));
        assertThrows(ServiceException.class, () -> PrescriptionUtils.requireItemCode("  "));
    }
}
