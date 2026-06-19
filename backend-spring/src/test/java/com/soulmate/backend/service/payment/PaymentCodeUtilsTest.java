package com.soulmate.backend.service.payment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentCodeUtilsTest {

    @Test
    void generateCreatesUppercaseAsciiPremiumCode() {
        String paymentCode = PaymentCodeUtils.generate();

        assertTrue(paymentCode.startsWith("SMPRM"));
        assertTrue(paymentCode.matches("SMPRM[A-Z0-9]{8}"));
    }

    @Test
    void extractFindsPremiumCodeAcrossDifferentMemoFormats() {
        assertEquals("SMPRMABC12345", PaymentCodeUtils.extract("transfer for smprmabc12345"));
        assertEquals("SMPRM1A2B3C4D", PaymentCodeUtils.extract("BANK SMS", "smprm1a2b3c4d thanh toan"));
        assertEquals("", PaymentCodeUtils.extract("khong co ma hop le"));
    }
}
