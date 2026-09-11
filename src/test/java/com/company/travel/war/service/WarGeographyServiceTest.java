package com.company.travel.war.service;

import com.company.travel.auth.exception.ResourceNotFoundException;
import com.company.travel.war.dto.CreateWarGeographyRequest;
import com.company.travel.war.entity.WarGeography;
import com.company.travel.war.exception.DuplicateWarGeographyException;
import com.company.travel.war.repository.WarGeographyRepository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WarGeographyServiceTest {

    private final WarGeographyRepository repository = mock(WarGeographyRepository.class);
    private final WarGeographyService service = new WarGeographyService(repository);

    @Test
    void createsCountryOnlyGeographyAndAllowsExistingRelatedGeography() {
        CreateWarGeographyRequest request = request(" us ", "   ", 7L, true);
        when(repository.existsById(7L)).thenReturn(true);
        when(repository.existsByCountryCodeAndRegionOrCity("US", null)).thenReturn(false);
        when(repository.save(any(WarGeography.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WarGeography result = service.create(request);

        assertEquals("US", result.getCountryCode());
        assertEquals(null, result.getRegionOrCity());
        assertEquals(7L, result.getRelatedToWarGeographyId());
    }

    @Test
    void rejectsInvalidCountryDuplicateAndUnknownRelatedGeography() {
        CreateWarGeographyRequest invalid = request("XXX", null, null, true);
        assertThrows(IllegalArgumentException.class, () -> service.create(invalid));

        CreateWarGeographyRequest unknown = request("CA", null, 8L, true);
        when(repository.existsById(8L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> service.create(unknown));

        CreateWarGeographyRequest duplicate = request("GB", "Capital", null, false);
        when(repository.existsByCountryCodeAndRegionOrCity("GB", "Capital")).thenReturn(true);
        assertThrows(DuplicateWarGeographyException.class, () -> service.create(duplicate));
    }

    private CreateWarGeographyRequest request(String country, String city,
            Long parentId, boolean active) {
        CreateWarGeographyRequest request = new CreateWarGeographyRequest();
        request.setCountryCode(country);
        request.setRegionOrCity(city);
        request.setRelatedToWarGeographyId(parentId);
        request.setActive(active);
        return request;
    }
}