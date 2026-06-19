package com.soulmate.backend.dto.payment;

public record WebhookAckResponse(
    boolean success,
    String message
) {
}
