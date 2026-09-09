package com.company.travel.payment;

import com.company.travel.auth.entity.User;
import com.company.travel.auth.repository.UserRepository;
import com.company.travel.payment.entity.Payment;
import com.company.travel.payment.dto.PaymentResponse;
import com.company.travel.payment.repository.PaymentRepository;
import com.company.travel.payment.service.PaymentService;
import com.company.travel.quotation.entity.Quotation;
import com.company.travel.quotation.repository.QuotationRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class PaymentDatabaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void paymentPersistsAgainstQuotationUsingFlywaySchema() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setUsername("payment-db-" + suffix);
        user.setEmail("payment-db-" + suffix + "@company.com");
        user.setPasswordHash(passwordEncoder.encode("Str0ng!Passw0rd123"));
        user.setFullName("Payment DB User");
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.saveAndFlush(user);

        Quotation quotation = new Quotation();
        quotation.setQuotationNumber("QT-2026-" + suffix);
        quotation.setTravellerName("Ravi Kumar");
        quotation.setTravellerDateOfBirth(LocalDate.of(1990, 1, 1));
        quotation.setPassportNumber("P1234567");
        quotation.setOriginCountry("LK");
        quotation.setDestinationCountry("FR");
        quotation.setDestinationCity("Paris");
        quotation.setTravelStartDate(LocalDate.of(2026, 9, 10));
        quotation.setTravelEndDate(LocalDate.of(2026, 9, 20));
        quotation.setCoverType("TRAVEL");
        quotation.setSumInsured(new BigDecimal("10000"));
        quotation.setCurrency("USD");
        quotation.setStatus("PAYMENT_PENDING");
        quotation.setCreatedByUserId(user.getId());
        quotation.setCreatedAt(LocalDateTime.now());
        quotation.setUpdatedAt(LocalDateTime.now());
        quotation = quotationRepository.saveAndFlush(quotation);

        PaymentResponse payment = paymentService.record(
            quotation.getId(), Payment.Outcome.SUCCESS, user.getUsername());
        paymentRepository.flush();

        Payment saved = paymentRepository.findByQuotationId(quotation.getId()).orElseThrow();
        assertTrue(payment.getId() != null);
        assertEquals(Payment.Outcome.SUCCESS, saved.getOutcome());
        assertEquals(quotation.getId(), saved.getQuotationId());
        assertEquals("PAYMENT_CONFIRMED", quotationRepository.findById(quotation.getId()).orElseThrow().getStatus());
    }
}
