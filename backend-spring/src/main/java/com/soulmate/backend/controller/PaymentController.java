package com.soulmate.backend.controller;

import com.soulmate.backend.dto.payment.CreatePremiumOrderResponse;
import com.soulmate.backend.dto.payment.PaymentOrderStatusResponse;
import com.soulmate.backend.dto.payment.PremiumOfferResponse;
import com.soulmate.backend.dto.payment.ReconcilePaymentOrderResponse;
import com.soulmate.backend.security.AuthContextHolder;
import com.soulmate.backend.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/secure/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/premium/offer")
    public PremiumOfferResponse getPremiumOffer(HttpServletRequest request) {
        String uid = AuthContextHolder.getRequired(request).uid();
        return paymentService.getPremiumOffer(uid);
    }

    @PostMapping("/premium/orders")
    public CreatePremiumOrderResponse createOrResumePremiumOrder(HttpServletRequest request) {
        String uid = AuthContextHolder.getRequired(request).uid();
        return paymentService.createOrResumePremiumOrder(uid);
    }

    @GetMapping("/orders/{orderId}")
    public PaymentOrderStatusResponse getPaymentOrderStatus(
        HttpServletRequest request,
        @PathVariable String orderId
    ) {
        String uid = AuthContextHolder.getRequired(request).uid();
        return paymentService.getOrderStatus(uid, orderId);
    }

    @PostMapping("/orders/{orderId}/reconcile")
    public ReconcilePaymentOrderResponse reconcilePaymentOrder(
        HttpServletRequest request,
        @PathVariable String orderId
    ) {
        String uid = AuthContextHolder.getRequired(request).uid();
        return paymentService.reconcilePaymentOrder(uid, orderId);
    }
}
