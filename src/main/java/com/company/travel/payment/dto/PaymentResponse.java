package com.company.travel.payment.dto;

import com.company.travel.payment.entity.Payment;

import java.time.LocalDateTime;

public class PaymentResponse {

    private Long id;
    private Long quotationId;
    private Payment.Outcome outcome;
    private LocalDateTime recordedAt;
    private LocalDateTime updatedAt;

    public PaymentResponse() {
    }

    public static PaymentResponse from(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.id = payment.getId();
        response.quotationId = payment.getQuotationId();
        response.outcome = payment.getOutcome();
        response.recordedAt = payment.getRecordedAt();
        response.updatedAt = payment.getUpdatedAt();
        return response;
    }

    public Long getId() {
        return id;
    }

    public Long getQuotationId() {
        return quotationId;
    }

    public Payment.Outcome getOutcome() {
        return outcome;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
