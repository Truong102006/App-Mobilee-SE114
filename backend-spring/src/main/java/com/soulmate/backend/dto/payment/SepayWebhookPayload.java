package com.soulmate.backend.dto.payment;

public record SepayWebhookPayload(
    long id,
    String gateway,
    String transactionDate,
    String accountNumber,
    String subAccount,
    String code,
    String content,
    String transferType,
    String description,
    long transferAmount,
    Long accumulated,
    String referenceCode
) {
}
