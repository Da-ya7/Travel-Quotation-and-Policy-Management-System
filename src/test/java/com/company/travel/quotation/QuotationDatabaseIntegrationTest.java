package com.company.travel.quotation;

import com.company.travel.auth.entity.User;
import com.company.travel.auth.repository.UserRepository;
import com.company.travel.quotation.entity.Quotation;
import com.company.travel.quotation.repository.QuotationRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class QuotationDatabaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void quotationPersistsWithForeignKeyOwnerAndOwnershipQueries() {
        User user = new User();
        user.setUsername("quotation-db-user");
        user.setEmail("quotation-db-user@company.com");
        user.setPasswordHash(passwordEncoder.encode("Str0ng!Passw0rd123"));
        user.setFullName("Quotation DB User");
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        Quotation quotation = new Quotation();
        quotation.setQuotationNumber("QT-2026-999999");
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
        quotation.setStatus("DRAFT");
        quotation.setCreatedByUserId(user.getId());
        quotation.setCreatedAt(LocalDateTime.now());
        quotation.setUpdatedAt(LocalDateTime.now());
        quotation = quotationRepository.saveAndFlush(quotation);

        assertTrue(quotationRepository.findByIdAndCreatedByUserId(
                quotation.getId(), user.getId()).isPresent());
        assertTrue(quotationRepository.findByIdAndCreatedByUserId(
                quotation.getId(), user.getId() + 1).isEmpty());
        assertEquals("DRAFT", quotationRepository.findById(quotation.getId()).orElseThrow().getStatus());
        assertEquals("USD", quotationRepository.findById(quotation.getId()).orElseThrow().getCurrency());
    }
}