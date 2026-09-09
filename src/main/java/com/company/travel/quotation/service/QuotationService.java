package com.company.travel.quotation.service;

import com.company.travel.auth.entity.User;
import com.company.travel.auth.exception.ResourceNotFoundException;
import com.company.travel.auth.service.UserService;
import com.company.travel.quotation.dto.CreateQuotationRequest;
import com.company.travel.quotation.dto.QuotationResponse;
import com.company.travel.quotation.entity.Quotation;
import com.company.travel.quotation.repository.QuotationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class QuotationService {

    private final QuotationRepository quotationRepository;
    private final UserService userService;
    private final String configuredCurrency;

    public QuotationService(
            QuotationRepository quotationRepository,
            UserService userService,
            @Value("${quotation.currency}") String configuredCurrency) {
        this.quotationRepository = quotationRepository;
        this.userService = userService;
        this.configuredCurrency = configuredCurrency;
    }

    @Transactional
    public QuotationResponse create(CreateQuotationRequest request, String username) {
        User user = authenticatedUser(username);

        if (request.getTravelEndDate().isBefore(request.getTravelStartDate())) {
            throw new IllegalArgumentException("travelEndDate must be on or after travelStartDate");
        }
        if (request.getTravelStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("travelStartDate must be today or later");
        }
        if (!isIsoCountryCode(request.getOriginCountry())) {
            throw new IllegalArgumentException("originCountry must be an ISO 3166-1 alpha-2 country code");
        }
        if (!isIsoCountryCode(request.getDestinationCountry())) {
            throw new IllegalArgumentException("destinationCountry must be an ISO 3166-1 alpha-2 country code");
        }

        LocalDateTime now = LocalDateTime.now();
        Quotation quotation = new Quotation();
        quotation.setQuotationNumber(generateQuotationNumber());
        quotation.setTravellerName(request.getTravellerName());
        quotation.setTravellerDateOfBirth(request.getTravellerDateOfBirth());
        quotation.setPassportNumber(request.getPassportNumber());
        quotation.setOriginCountry(request.getOriginCountry());
        quotation.setDestinationCountry(request.getDestinationCountry());
        quotation.setDestinationCity(request.getDestinationCity());
        quotation.setTravelStartDate(request.getTravelStartDate());
        quotation.setTravelEndDate(request.getTravelEndDate());
        quotation.setCoverType(request.getCoverType());
        quotation.setSumInsured(request.getSumInsured());
        quotation.setPremium(null);
        quotation.setCurrency(configuredCurrency);
        quotation.setStatus("DRAFT");
        quotation.setCreatedByUserId(user.getId());
        quotation.setCreatedAt(now);
        quotation.setUpdatedAt(now);

        return QuotationResponse.from(quotationRepository.save(quotation));
    }

    @Transactional(readOnly = true)
    public List<QuotationResponse> findOwn(String username) {
        Long userId = authenticatedUser(username).getId();
        return quotationRepository.findByCreatedByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(QuotationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuotationResponse findOwnById(Long quotationId, String username) {
        Long userId = authenticatedUser(username).getId();
        return quotationRepository.findByIdAndCreatedByUserId(quotationId, userId)
                .map(QuotationResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RESOURCE_NOT_OWNED",
                        "Quotation is not owned by the authenticated user"));
    }

    private User authenticatedUser(String username) {
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("USER_NOT_FOUND", "User not found: " + username);
        }
        return user;
    }

    private String generateQuotationNumber() {
        int year = LocalDate.now().getYear();
        for (int attempt = 0; attempt < 10; attempt++) {
            int sequence = ThreadLocalRandom.current().nextInt(0, 1_000_000);
            String number = String.format("QT-%04d-%06d", year, sequence);
            if (!quotationRepository.existsByQuotationNumber(number)) {
                return number;
            }
        }
        throw new IllegalStateException("Unable to generate a unique quotation number");
    }

    private boolean isIsoCountryCode(String countryCode) {
        return Arrays.asList(Locale.getISOCountries()).contains(countryCode);
    }
}