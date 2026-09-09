package com.company.travel.payment.service;

import com.company.travel.auth.entity.User;
import com.company.travel.auth.exception.ResourceNotFoundException;
import com.company.travel.auth.service.UserService;
import com.company.travel.payment.dto.PaymentResponse;
import com.company.travel.payment.entity.Payment;
import com.company.travel.payment.repository.PaymentRepository;
import com.company.travel.quotation.entity.Quotation;
import com.company.travel.quotation.repository.QuotationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    private PaymentRepository paymentRepository;
    private QuotationRepository quotationRepository;
    private UserService userService;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        quotationRepository = mock(QuotationRepository.class);
        userService = mock(UserService.class);
        paymentService = new PaymentService(paymentRepository, quotationRepository, userService);
    }

    @Test
    void recordsSuccessfulPaymentForOwnQuotation() {
        givenOwnQuotation();
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> saved(invocation.getArgument(0)));

        PaymentResponse response = paymentService.record(42L, Payment.Outcome.SUCCESS, "uw.ravi");

        assertEquals(42L, response.getQuotationId());
        assertEquals(Payment.Outcome.SUCCESS, response.getOutcome());
        assertNotNull(response.getRecordedAt());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void recordsFailedPaymentForOwnQuotation() {
        givenOwnQuotation();
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> saved(invocation.getArgument(0)));

        PaymentResponse response = paymentService.record(42L, Payment.Outcome.FAILED, "uw.ravi");

        assertEquals(Payment.Outcome.FAILED, response.getOutcome());
    }

    @Test
    void repeatedPaymentUpdatesExistingPaymentDeterministically() {
        givenOwnQuotation();
        Payment existing = new Payment();
        existing.setId(7L);
        existing.setQuotationId(42L);
        existing.setOutcome(Payment.Outcome.SUCCESS);
        existing.setRecordedAt(LocalDateTime.now().minusMinutes(1));
        existing.setUpdatedAt(existing.getRecordedAt());
        when(paymentRepository.findByQuotationId(42L)).thenReturn(Optional.of(existing));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.record(42L, Payment.Outcome.FAILED, "uw.ravi");

        assertEquals(7L, response.getId());
        assertEquals(Payment.Outcome.FAILED, response.getOutcome());
        assertEquals(existing.getRecordedAt(), response.getRecordedAt());
    }

    @Test
    void foreignQuotationIsNotExposed() {
        when(userService.findByUsername("uw.ravi")).thenReturn(user(7L));
        when(quotationRepository.findByIdAndCreatedByUserId(42L, 7L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> paymentService.record(42L, Payment.Outcome.SUCCESS, "uw.ravi"));

        assertEquals("RESOURCE_NOT_OWNED", exception.getErrorCode());
    }

    @Test
    void nonexistentQuotationUsesOwnershipNotFoundResponse() {
        when(userService.findByUsername("uw.ravi")).thenReturn(user(7L));
        when(quotationRepository.findByIdAndCreatedByUserId(999L, 7L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> paymentService.record(999L, Payment.Outcome.FAILED, "uw.ravi"));

        assertEquals("RESOURCE_NOT_OWNED", exception.getErrorCode());
    }

    private void givenOwnQuotation() {
        when(userService.findByUsername("uw.ravi")).thenReturn(user(7L));
        Quotation quotation = new Quotation();
        quotation.setId(42L);
        quotation.setCreatedByUserId(7L);
        when(quotationRepository.findByIdAndCreatedByUserId(42L, 7L)).thenReturn(Optional.of(quotation));
        when(paymentRepository.findByQuotationId(42L)).thenReturn(Optional.empty());
    }

    private Payment saved(Payment payment) {
        payment.setId(1L);
        return payment;
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("uw.ravi");
        return user;
    }
}
