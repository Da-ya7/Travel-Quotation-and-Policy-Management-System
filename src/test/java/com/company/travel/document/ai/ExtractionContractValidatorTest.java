package com.company.travel.document.ai;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExtractionContractValidatorTest {
    @Test
    void acceptsCompleteTravelContractWithNullValues() {
        Map<String, Object> fields = new HashMap<>();
        fields.put("passenger", null);
        fields.put("origin", null);
        fields.put("destinationCountry", "FR");
        fields.put("destinationCityOrAirport", null);
        fields.put("departureDate", "2026-09-10");
        fields.put("returnOrArrivalDate", "2026-09-20");
        assertDoesNotThrow(() -> ExtractionContractValidator.validate(fields, "TRAVEL_TICKET"));
    }

    @Test
    void rejectsMissingUnknownOrWronglyTypedFields() {
        assertThrows(IllegalArgumentException.class,
                () -> ExtractionContractValidator.validate(Map.of("passenger", "Ravi"), "TRAVEL_TICKET"));
        Map<String, Object> fields = new HashMap<>();
        fields.put("passenger", 42);
        fields.put("origin", null);
        fields.put("destinationCountry", null);
        fields.put("destinationCityOrAirport", null);
        fields.put("departureDate", null);
        fields.put("returnOrArrivalDate", null);
        assertThrows(IllegalArgumentException.class,
                () -> ExtractionContractValidator.validate(fields, "TRAVEL_TICKET"));
    }

    @Test
    void acceptsUnknownWithEmptyFields() {
        assertDoesNotThrow(() -> ExtractionContractValidator.validate(Map.of(), "UNKNOWN"));
    }

    @Test
    void rejectsUnknownWithFields() {
        assertThrows(IllegalArgumentException.class,
                () -> ExtractionContractValidator.validate(Map.of("guest", "Ravi"), "UNKNOWN"));
    }
}