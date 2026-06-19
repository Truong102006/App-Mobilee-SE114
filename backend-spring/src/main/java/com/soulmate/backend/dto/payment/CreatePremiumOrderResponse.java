package com.soulmate.backend.dto.payment;

public record CreatePremiumOrderResponse(
    String orderId,
    String status,
    String planCode,
    long amountVnd,
    int durationDays,
    String paymentCode,
    long expiresAt,
    String qrImageUrl,
    String bankCode,
    String bankAccount,
    String accountHolder
) {
}
