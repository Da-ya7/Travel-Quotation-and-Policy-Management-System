package com.company.travel.war.dto;

import com.company.travel.war.entity.WarGeography;

public record WarGeographyResponse(
        Long id,
        String countryCode,
        String regionOrCity,
        Long relatedToWarGeographyId,
        boolean active) {

    public static WarGeographyResponse from(WarGeography geography) {
        return new WarGeographyResponse(
                geography.getId(),
                geography.getCountryCode(),
                geography.getRegionOrCity(),
                geography.getRelatedToWarGeographyId(),
                geography.isActive());
    }
}