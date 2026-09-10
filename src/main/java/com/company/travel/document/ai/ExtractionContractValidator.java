package com.company.travel.document.ai;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

public final class ExtractionContractValidator {

    private static final Set<String> TRAVEL_FIELDS = Set.of(
            "passenger", "origin", "destinationCountry", "destinationCityOrAirport",
            "departureDate", "returnOrArrivalDate");
    private static final Set<String> HOTEL_FIELDS = Set.of(
            "guest", "hotelCountry", "hotelCity", "checkInDate", "checkOutDate");

    private ExtractionContractValidator() {
    }

    public static void validate(Map<String, Object> fields, String documentType) {
        Set<String> expected = switch (documentType) {
            case "TRAVEL_TICKET" -> TRAVEL_FIELDS;
            case "HOTEL_BOOKING" -> HOTEL_FIELDS;
            case "UNKNOWN" -> Set.of();
            default -> throw new IllegalArgumentException("Malformed document AI response");
        };
        if (!fields.keySet().equals(expected)) {
            throw new IllegalArgumentException("Malformed document AI response");
        }
        for (String field : expected) {
            Object value = fields.get(field);
            if (value != null && !(value instanceof String)) {
                throw new IllegalArgumentException("Malformed document AI response");
            }
        }
        for (String dateField : documentType.equals("TRAVEL_TICKET")
                ? Set.of("departureDate", "returnOrArrivalDate")
                : Set.of("checkInDate", "checkOutDate")) {
            String value = (String) fields.get(dateField);
            if (value != null) {
                try {
                    LocalDate.parse(value);
                } catch (RuntimeException exception) {
                    throw new IllegalArgumentException("Malformed document AI response", exception);
                }
            }
        }
    }
}