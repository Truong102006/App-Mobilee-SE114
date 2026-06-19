package com.soulmate.backend.controller;

import com.soulmate.backend.dto.payment.WebhookAckResponse;
import com.soulmate.backend.service.PaymentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/payments/sepay")
public class PublicPaymentController {

    private final PaymentService paymentService;

    public PublicPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/webhook")
    public WebhookAckResponse receiveWebhook(
        @RequestBody String rawBody,
        @RequestHeader(name = "X-SePay-Signature", required = false) String signatureHeader,
        @RequestHeader(name = "X-SePay-Timestamp", required = false) String timestampHeader
    ) {
        return paymentService.handleSepayWebhook(rawBody, signatureHeader, timestampHeader);
    }
}
