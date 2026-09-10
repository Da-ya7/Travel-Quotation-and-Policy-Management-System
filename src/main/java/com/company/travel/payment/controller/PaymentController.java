package com.company.travel.payment.controller;

import com.company.travel.payment.dto.PaymentResponse;
import com.company.travel.payment.entity.Payment;
import com.company.travel.payment.service.PaymentService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/quotations/{quotationId}/payment")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping({ "", "/initiate" })
    @PreAuthorize("hasAuthority('PAYMENT_COLLECT')")
    public ResponseEntity<PaymentResponse> initiate(
            @PathVariable Long quotationId,
            Authentication authentication) {
        return ResponseEntity.ok(paymentService.initiate(quotationId, authentication.getName()));
    }

    @PostMapping("/success")
    @PreAuthorize("hasAuthority('PAYMENT_COLLECT')")
    public ResponseEntity<PaymentResponse> success(
            @PathVariable Long quotationId,
            Authentication authentication) {
        return ResponseEntity.ok(paymentService.record(
                quotationId, Payment.Outcome.SUCCESS, authentication.getName()));
    }

    @PostMapping("/failed")
    @PreAuthorize("hasAuthority('PAYMENT_COLLECT')")
    public ResponseEntity<PaymentResponse> failed(
            @PathVariable Long quotationId,
            Authentication authentication) {
        return ResponseEntity.ok(paymentService.record(
                quotationId, Payment.Outcome.FAILED, authentication.getName()));
    }
}
