package com.company.travel.quotation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateQuotationRequest {

    @NotBlank
    private String travellerName;

    @NotNull
    @PastOrPresent
    private LocalDate travellerDateOfBirth;

    @NotBlank
    private String passportNumber;

    @NotBlank
    @Pattern(regexp = "[A-Z]{2}", message = "must be an ISO 3166-1 alpha-2 country code")
    private String originCountry;

    @NotBlank
    @Pattern(regexp = "[A-Z]{2}", message = "must be an ISO 3166-1 alpha-2 country code")
    private String destinationCountry;

    @NotBlank
    private String destinationCity;

    @NotNull
    private LocalDate travelStartDate;

    @NotNull
    private LocalDate travelEndDate;

    @NotBlank
    private String coverType;

    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal sumInsured;

    public CreateQuotationRequest() {
    }

    public String getTravellerName() {
        return travellerName;
    }

    public void setTravellerName(String travellerName) {
        this.travellerName = travellerName;
    }

    public LocalDate getTravellerDateOfBirth() {
        return travellerDateOfBirth;
    }

    public void setTravellerDateOfBirth(LocalDate travellerDateOfBirth) {
        this.travellerDateOfBirth = travellerDateOfBirth;
    }

    public String getPassportNumber() {
        return passportNumber;
    }

    public void setPassportNumber(String passportNumber) {
        this.passportNumber = passportNumber;
    }

    public String getOriginCountry() {
        return originCountry;
    }

    public void setOriginCountry(String originCountry) {
        this.originCountry = originCountry;
    }

    public String getDestinationCountry() {
        return destinationCountry;
    }

    public void setDestinationCountry(String destinationCountry) {
        this.destinationCountry = destinationCountry;
    }

    public String getDestinationCity() {
        return destinationCity;
    }

    public void setDestinationCity(String destinationCity) {
        this.destinationCity = destinationCity;
    }

    public LocalDate getTravelStartDate() {
        return travelStartDate;
    }

    public void setTravelStartDate(LocalDate travelStartDate) {
        this.travelStartDate = travelStartDate;
    }

    public LocalDate getTravelEndDate() {
        return travelEndDate;
    }

    public void setTravelEndDate(LocalDate travelEndDate) {
        this.travelEndDate = travelEndDate;
    }

    public String getCoverType() {
        return coverType;
    }

    public void setCoverType(String coverType) {
        this.coverType = coverType;
    }

    public BigDecimal getSumInsured() {
        return sumInsured;
    }

    public void setSumInsured(BigDecimal sumInsured) {
        this.sumInsured = sumInsured;
    }
}