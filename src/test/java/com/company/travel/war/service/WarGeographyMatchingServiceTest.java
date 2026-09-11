package com.company.travel.war.service;

import com.company.travel.war.entity.WarGeography;
import com.company.travel.war.repository.WarGeographyRepository;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WarGeographyMatchingServiceTest {

    private final WarGeographyRepository repository = mock(WarGeographyRepository.class);
    private final WarGeographyMatchingService service = new WarGeographyMatchingService(repository);

    @Test
    void activeCountryAndCityGeographiesMatch() {
        WarGeography country = geography(1L, "US", null, null, true);
        WarGeography city = geography(2L, "CA", "Capital City", null, true);
        when(repository.findAllByActiveTrue()).thenReturn(List.of(country, city));

        assertTrue(service.match("us", "Any City").matched());
        assertTrue(service.match("CA", "capital city").matched());
    }

    @Test
    void inactiveAndNonMatchingGeographiesDoNotMatch() {
        when(repository.findAllByActiveTrue()).thenReturn(List.of());

        assertFalse(service.match("US", "Capital City").matched());
    }

    @Test
    void activeRelatedGeographyMatchesOnlyWhenParentIsActive() {
        WarGeography activeParent = geography(1L, "US", null, null, true);
        WarGeography activeChild = geography(2L, "CA", null, 1L, true);
        when(repository.findAllByActiveTrue()).thenReturn(List.of(activeParent, activeChild));
        assertTrue(service.match("CA", "Any City").matched());

        WarGeography childOfInactiveParent = geography(3L, "GB", null, 99L, true);
        when(repository.findAllByActiveTrue()).thenReturn(List.of(childOfInactiveParent));
        assertFalse(service.match("GB", "Any City").matched());
    }

    @Test
    void inactiveChildrenAndRelatedGeographiesOfRelatedParentsDoNotMatch() {
        WarGeography activeParent = geography(1L, "US", null, null, true);
        WarGeography inactiveChild = geography(2L, "CA", null, 1L, false);
        WarGeography activeChild = geography(3L, "MX", null, 1L, true);
        WarGeography grandchild = geography(4L, "GB", null, 3L, true);
        when(repository.findAllByActiveTrue())
                .thenReturn(List.of(activeParent, inactiveChild, activeChild, grandchild));

        assertFalse(service.match("CA", "Any City").matched());
        assertFalse(service.match("GB", "Any City").matched());
    }

    @Test
    void differentCityDoesNotMatchCitySpecificGeography() {
        WarGeography city = geography(1L, "US", "New York", null, true);
        when(repository.findAllByActiveTrue()).thenReturn(List.of(city));

        assertFalse(service.match("US", "Boston").matched());
    }

    private WarGeography geography(Long id, String country, String city,
            Long parentId, boolean active) {
        WarGeography geography = new WarGeography();
        geography.setId(id);
        geography.setCountryCode(country);
        geography.setRegionOrCity(city);
        geography.setRelatedToWarGeographyId(parentId);
        geography.setActive(active);
        return geography;
    }
}