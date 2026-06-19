package com.soulmate.backend.dto.payment;

public record PaymentOrderStatusResponse(
    String orderId,
    String status,
    String planCode,
    long amountVnd,
    int durationDays,
    String paymentCode,
    long createdAt,
    long expiresAt,
    Long paidAt,
    Long premiumGrantedUntil,
    boolean latePayment
) {
}
