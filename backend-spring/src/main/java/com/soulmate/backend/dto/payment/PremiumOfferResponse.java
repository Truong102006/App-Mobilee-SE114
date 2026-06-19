package com.soulmate.backend.dto.payment;

public record PremiumOfferResponse(
    String planCode,
    long priceVnd,
    int durationDays,
    int orderExpireMinutes,
    String bankCode,
    String bankAccount,
    String accountHolder,
    Long currentPremiumUntil
) {
}
