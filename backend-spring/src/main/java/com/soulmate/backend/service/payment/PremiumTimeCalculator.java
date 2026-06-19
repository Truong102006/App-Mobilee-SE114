package com.soulmate.backend.service.payment;

import java.time.Duration;

public final class PremiumTimeCalculator {

    private PremiumTimeCalculator() {
    }

    public static long computeGrantedUntil(Long currentPremiumUntil, long paymentTimeMillis, int durationDays) {
        long base = paymentTimeMillis;
        if (currentPremiumUntil != null && currentPremiumUntil > paymentTimeMillis) {
            base = currentPremiumUntil;
        }
        return base + Duration.ofDays(durationDays).toMillis();
    }

    public static boolean isWithinLateWindow(long expiresAt, long paymentTimeMillis, int reconcileWindowHours) {
        long latestAcceptedTime = expiresAt + Duration.ofHours(reconcileWindowHours).toMillis();
        return paymentTimeMillis <= latestAcceptedTime;
    }
}
