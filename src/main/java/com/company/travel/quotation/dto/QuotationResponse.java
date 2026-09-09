package com.company.travel.quotation.dto;

import com.company.travel.quotation.entity.Quotation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class QuotationResponse {

    private Long id;
    private String quotationNumber;
    private String travellerName;
    private LocalDate travellerDateOfBirth;
    private String passportNumber;
    private String originCountry;
    private String destinationCountry;
    private String destinationCity;
    private LocalDate travelStartDate;
    private LocalDate travelEndDate;
    private String coverType;
    private BigDecimal sumInsured;
    private BigDecimal premium;
    private String currency;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public QuotationResponse() {
    }

    public QuotationResponse(Long id, String quotationNumber, String status) {
        this.id = id;
        this.quotationNumber = quotationNumber;
        this.status = status;
    }

    public static QuotationResponse from(Quotation quotation) {
        QuotationResponse response = new QuotationResponse();
        response.id = quotation.getId();
        response.quotationNumber = quotation.getQuotationNumber();
        response.travellerName = quotation.getTravellerName();
        response.travellerDateOfBirth = quotation.getTravellerDateOfBirth();
        response.passportNumber = quotation.getPassportNumber();
        response.originCountry = quotation.getOriginCountry();
        response.destinationCountry = quotation.getDestinationCountry();
        response.destinationCity = quotation.getDestinationCity();
        response.travelStartDate = quotation.getTravelStartDate();
        response.travelEndDate = quotation.getTravelEndDate();
        response.coverType = quotation.getCoverType();
        response.sumInsured = quotation.getSumInsured();
        response.premium = quotation.getPremium();
        response.currency = quotation.getCurrency();
        response.status = quotation.getStatus();
        response.createdAt = quotation.getCreatedAt();
        response.updatedAt = quotation.getUpdatedAt();
        return response;
    }

    public Long getId() {
        return id;
    }

    public String getQuotationNumber() {
        return quotationNumber;
    }

    public String getTravellerName() {
        return travellerName;
    }

    public LocalDate getTravellerDateOfBirth() {
        return travellerDateOfBirth;
    }

    public String getPassportNumber() {
        return passportNumber;
    }

    public String getOriginCountry() {
        return originCountry;
    }

    public String getDestinationCountry() {
        return destinationCountry;
    }

    public String getDestinationCity() {
        return destinationCity;
    }

    public LocalDate getTravelStartDate() {
        return travelStartDate;
    }

    public LocalDate getTravelEndDate() {
        return travelEndDate;
    }

    public String getCoverType() {
        return coverType;
    }

    public BigDecimal getSumInsured() {
        return sumInsured;
    }

    public BigDecimal getPremium() {
        return premium;
    }

    public String getCurrency() {
        return currency;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}