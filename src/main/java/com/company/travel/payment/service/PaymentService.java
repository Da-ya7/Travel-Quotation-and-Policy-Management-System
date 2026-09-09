package com.company.travel.payment.service;

import com.company.travel.auth.exception.ResourceNotFoundException;
import com.company.travel.auth.service.UserService;
import com.company.travel.payment.dto.PaymentResponse;
import com.company.travel.payment.entity.Payment;
import com.company.travel.payment.repository.PaymentRepository;
import com.company.travel.quotation.entity.Quotation;
import com.company.travel.quotation.repository.QuotationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final QuotationRepository quotationRepository;
    private final UserService userService;

    public PaymentService(
            PaymentRepository paymentRepository,
            QuotationRepository quotationRepository,
            UserService userService) {
        this.paymentRepository = paymentRepository;
        this.quotationRepository = quotationRepository;
        this.userService = userService;
    }

    @Transactional
    public PaymentResponse record(Long quotationId, Payment.Outcome outcome, String username) {
        Long userId = userService.findByUsername(username).getId();
        Quotation quotation = quotationRepository.findByIdAndCreatedByUserId(quotationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RESOURCE_NOT_OWNED",
                        "Quotation is not owned by the authenticated user"));

        if (!"PAYMENT_PENDING".equals(quotation.getStatus())) {
            throw new IllegalArgumentException(
                "Payment can only be recorded for quotations in PAYMENT_PENDING status");
        }

        LocalDateTime now = LocalDateTime.now();
        Payment payment = paymentRepository.findByQuotationId(quotation.getId())
                .orElseGet(() -> {
                    Payment newPayment = new Payment();
                    newPayment.setQuotationId(quotation.getId());
                    newPayment.setRecordedAt(now);
                    return newPayment;
                });
        payment.setOutcome(outcome);
        payment.setUpdatedAt(now);

        Payment savedPayment = paymentRepository.save(payment);
        if (outcome == Payment.Outcome.SUCCESS) {
            quotation.setStatus("PAYMENT_CONFIRMED");
            quotation.setUpdatedAt(now);
        }
        quotationRepository.save(quotation);

        return PaymentResponse.from(savedPayment);
    }
}
