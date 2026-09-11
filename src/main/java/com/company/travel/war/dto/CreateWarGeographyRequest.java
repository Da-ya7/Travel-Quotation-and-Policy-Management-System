package com.company.travel.war.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateWarGeographyRequest {

    @NotBlank
    private String countryCode;

    @Size(max = 100)
    private String regionOrCity;

    private Long relatedToWarGeographyId;

    @NotNull
    private Boolean active;

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getRegionOrCity() {
        return regionOrCity;
    }

    public void setRegionOrCity(String regionOrCity) {
        this.regionOrCity = regionOrCity;
    }

    public Long getRelatedToWarGeographyId() {
        return relatedToWarGeographyId;
    }

    public void setRelatedToWarGeographyId(Long relatedToWarGeographyId) {
        this.relatedToWarGeographyId = relatedToWarGeographyId;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}