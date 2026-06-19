package com.soulmate.backend.dto.payment;

public record ReconcilePaymentOrderResponse(
    boolean matched,
    String message,
    PaymentOrderStatusResponse order
) {
}
