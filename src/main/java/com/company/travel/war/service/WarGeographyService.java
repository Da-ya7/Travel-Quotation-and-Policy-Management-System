package com.company.travel.war.service;

import com.company.travel.auth.exception.ResourceNotFoundException;
import com.company.travel.war.dto.CreateWarGeographyRequest;
import com.company.travel.war.entity.WarGeography;
import com.company.travel.war.exception.DuplicateWarGeographyException;
import com.company.travel.war.repository.WarGeographyRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Locale;

@Service
public class WarGeographyService {

    private final WarGeographyRepository repository;

    public WarGeographyService(WarGeographyRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public WarGeography create(CreateWarGeographyRequest request) {
        String countryCode = normalizeCountryCode(request.getCountryCode());
        if (!isIsoCountryCode(countryCode)) {
            throw new IllegalArgumentException(
                    "countryCode must be an ISO 3166-1 alpha-2 country code");
        }
        String regionOrCity = normalizeRegionOrCity(request.getRegionOrCity());

        if (request.getRelatedToWarGeographyId() != null
                && !repository.existsById(request.getRelatedToWarGeographyId())) {
            throw new ResourceNotFoundException(
                    "WAR_GEOGRAPHY_NOT_FOUND", "Related war geography was not found");
        }

        if (repository.existsByCountryCodeAndRegionOrCity(
                countryCode, regionOrCity)) {
            throw new DuplicateWarGeographyException(
                    "War geography country and region/city already exists");
        }

        WarGeography geography = new WarGeography();
        geography.setCountryCode(countryCode);
        geography.setRegionOrCity(regionOrCity);
        geography.setRelatedToWarGeographyId(request.getRelatedToWarGeographyId());
        geography.setActive(request.getActive());
        return repository.save(geography);
    }

    private boolean isIsoCountryCode(String countryCode) {
        return countryCode != null
                && Arrays.asList(Locale.getISOCountries()).contains(countryCode);
    }

    private String normalizeCountryCode(String countryCode) {
        return countryCode == null ? null : countryCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeRegionOrCity(String regionOrCity) {
        if (regionOrCity == null) {
            return null;
        }
        String normalized = regionOrCity.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}