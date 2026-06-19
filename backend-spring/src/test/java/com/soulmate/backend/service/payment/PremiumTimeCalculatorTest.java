package com.soulmate.backend.service.payment;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PremiumTimeCalculatorTest {

    @Test
    void computeGrantedUntilRenewsFromCurrentPremiumWhenStillActive() {
        long paymentTime = 1_700_000_000_000L;
        long activePremiumUntil = paymentTime + Duration.ofDays(3).toMillis();

        long grantedUntil = PremiumTimeCalculator.computeGrantedUntil(activePremiumUntil, paymentTime, 30);

        assertEquals(activePremiumUntil + Duration.ofDays(30).toMillis(), grantedUntil);
    }

    @Test
    void computeGrantedUntilStartsFromPaymentTimeWhenPremiumExpired() {
        long paymentTime = 1_700_000_000_000L;
        long expiredPremiumUntil = paymentTime - Duration.ofDays(1).toMillis();

        long grantedUntil = PremiumTimeCalculator.computeGrantedUntil(expiredPremiumUntil, paymentTime, 30);

        assertEquals(paymentTime + Duration.ofDays(30).toMillis(), grantedUntil);
    }

    @Test
    void isWithinLateWindowUsesExpireTimePlusConfiguredHours() {
        long expiresAt = 1_700_000_000_000L;

        assertTrue(PremiumTimeCalculator.isWithinLateWindow(
            expiresAt,
            expiresAt + Duration.ofHours(23).toMillis(),
            24
        ));
        assertFalse(PremiumTimeCalculator.isWithinLateWindow(
            expiresAt,
            expiresAt + Duration.ofHours(25).toMillis(),
            24
        ));
    }
}
