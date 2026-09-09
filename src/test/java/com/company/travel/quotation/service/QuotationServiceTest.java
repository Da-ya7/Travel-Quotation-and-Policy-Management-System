package com.company.travel.quotation.service;

import com.company.travel.auth.entity.User;
import com.company.travel.auth.exception.ResourceNotFoundException;
import com.company.travel.auth.service.UserService;
import com.company.travel.quotation.dto.CreateQuotationRequest;
import com.company.travel.quotation.dto.QuotationResponse;
import com.company.travel.quotation.entity.Quotation;
import com.company.travel.quotation.repository.QuotationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuotationServiceTest {

    private QuotationRepository quotationRepository;
    private UserService userService;
    private QuotationService quotationService;

    @BeforeEach
    void setUp() {
        quotationRepository = mock(QuotationRepository.class);
        userService = mock(UserService.class);
        quotationService = new QuotationService(quotationRepository, userService, "USD");
    }

    @Test
    void createsDraftOwnedByAuthenticatedUserWithGeneratedNumber() {
        User user = user(7L, "uw.ravi");
        when(userService.findByUsername("uw.ravi")).thenReturn(user);
        when(quotationRepository.existsByQuotationNumber(any())).thenReturn(false);
        when(quotationRepository.save(any(Quotation.class)))
                .thenAnswer(invocation -> {
                    Quotation quotation = invocation.getArgument(0);
                    quotation.setId(10L);
                    return quotation;
                });

        QuotationResponse result = quotationService.create(request(), "uw.ravi");

        assertEquals(10L, result.getId());
        assertEquals("DRAFT", result.getStatus());
        assertEquals("USD", result.getCurrency());
        assertEquals("2026", result.getQuotationNumber().substring(3, 7));
        assertEquals(14, result.getQuotationNumber().length());
        verify(quotationRepository).save(any(Quotation.class));
    }

    @Test
    void dateRangeAndStartDateAreValidated() {
        when(userService.findByUsername("uw.ravi")).thenReturn(user(7L, "uw.ravi"));

        CreateQuotationRequest reversed = request();
        reversed.setTravelStartDate(LocalDate.of(2026, 9, 20));
        reversed.setTravelEndDate(LocalDate.of(2026, 9, 19));
        assertThrows(IllegalArgumentException.class,
                () -> quotationService.create(reversed, "uw.ravi"));

        CreateQuotationRequest inPast = request();
        inPast.setTravelStartDate(LocalDate.of(2026, 9, 8));
        inPast.setTravelEndDate(LocalDate.of(2026, 9, 10));
        assertThrows(IllegalArgumentException.class,
                () -> quotationService.create(inPast, "uw.ravi"));
    }

    @Test
    void listsOnlyAuthenticatedUsersQuotations() {
        when(userService.findByUsername("uw.ravi")).thenReturn(user(7L, "uw.ravi"));
        Quotation quotation = quotation(11L, 7L);
        when(quotationRepository.findByCreatedByUserIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(quotation));

        List<QuotationResponse> result = quotationService.findOwn("uw.ravi");

        assertEquals(List.of(11L), result.stream().map(QuotationResponse::getId).toList());
        verify(quotationRepository).findByCreatedByUserIdOrderByCreatedAtDesc(7L);
    }

    @Test
    void unownedQuotationReturnsResourceNotOwned() {
        when(userService.findByUsername("uw.other")).thenReturn(user(8L, "uw.other"));
        when(quotationRepository.findByIdAndCreatedByUserId(11L, 8L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> quotationService.findOwnById(11L, "uw.other"));

        assertEquals("RESOURCE_NOT_OWNED", exception.getErrorCode());
    }

    private CreateQuotationRequest request() {
        CreateQuotationRequest request = new CreateQuotationRequest();
        request.setTravellerName("Ravi Kumar");
        request.setTravellerDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setPassportNumber("P1234567");
        request.setOriginCountry("LK");
        request.setDestinationCountry("FR");
        request.setDestinationCity("Paris");
        request.setTravelStartDate(LocalDate.of(2026, 9, 10));
        request.setTravelEndDate(LocalDate.of(2026, 9, 20));
        request.setCoverType("TRAVEL");
        request.setSumInsured(new BigDecimal("10000"));
        return request;
    }

    private User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private Quotation quotation(Long id, Long ownerId) {
        Quotation quotation = new Quotation();
        quotation.setId(id);
        quotation.setCreatedByUserId(ownerId);
        quotation.setQuotationNumber("QT-2026-000001");
        quotation.setStatus("DRAFT");
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
        return quotation;
    }
}