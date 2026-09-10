package com.company.travel.document.service;

import com.company.travel.auth.entity.User;
import com.company.travel.auth.exception.ResourceNotFoundException;
import com.company.travel.auth.service.UserService;
import com.company.travel.document.ai.DocumentAiClient;
import com.company.travel.document.ai.DocumentAiExtraction;
import com.company.travel.document.entity.Document;
import com.company.travel.document.repository.DocumentRepository;
import com.company.travel.document.storage.DocumentStorage;
import com.company.travel.quotation.entity.Quotation;
import com.company.travel.quotation.repository.QuotationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentServiceTest {
    private DocumentRepository documentRepository;
    private QuotationRepository quotationRepository;
    private UserService userService;
    private DocumentStorage documentStorage;
    private DocumentAiClient documentAiClient;
    private DocumentValidationPersistence validationPersistence;
    private DocumentService service;

    @BeforeEach
    void setUp() {
        documentRepository = mock(DocumentRepository.class);
        quotationRepository = mock(QuotationRepository.class);
        userService = mock(UserService.class);
        documentStorage = mock(DocumentStorage.class);
        documentAiClient = mock(DocumentAiClient.class);
        validationPersistence = mock(DocumentValidationPersistence.class);
        service = new DocumentService(documentRepository, quotationRepository, userService,
                documentStorage, documentAiClient, validationPersistence, 0, 0.8);
    }

    @Test
    void uploadPersistsUploadedDocument() throws Exception {
        User user = user();
        Quotation quotation = quotation();
        when(userService.findByUsername("ravi")).thenReturn(user);
        when(quotationRepository.findByIdAndCreatedByUserId(7L, 1L)).thenReturn(Optional.of(quotation));
        when(documentStorage.store("ticket.png", new byte[] { 1 })).thenReturn("stored-ticket");
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        var response = service.upload(7L, new MockMultipartFile("document", "ticket.png",
                "image/png", new byte[] { 1 }), "TRAVEL_TICKET", "ravi");

        assertEquals("UPLOADED", response.status());
        assertEquals(null, response.extractedJson());
        verify(documentRepository).save(any(Document.class));
    }

    @Test
    void foreignQuotationFailsOwnershipCheck() {
        when(userService.findByUsername("ravi")).thenReturn(user());
        when(quotationRepository.findByIdAndCreatedByUserId(7L, 1L)).thenReturn(Optional.empty());

        assertEquals("RESOURCE_NOT_OWNED", assertThrows(ResourceNotFoundException.class,
                () -> service.validate(7L, "ravi")).getErrorCode());
    }

    @Test
    void mismatchIsPersistedAsInvalidBeforeException() throws Exception {
        Document document = uploadedDocument();
        when(userService.findByUsername("ravi")).thenReturn(user());
        when(quotationRepository.findByIdAndCreatedByUserId(7L, 1L)).thenReturn(Optional.of(quotation()));
        when(documentRepository.findByQuotationIdOrderByUploadedAtAsc(7L)).thenReturn(List.of(document));
        when(documentStorage.retrieve("stored-ticket")).thenReturn(new byte[] { 1 });
        when(documentAiClient.extract(any(), eq("ticket.png"), eq("image/png"), eq("TRAVEL_TICKET")))
                .thenReturn(new DocumentAiExtraction("TRAVEL_TICKET",
                        Map.of("passenger", "Other Person", "origin", "Colombo", "destinationCountry", "LK",
                                "destinationCityOrAirport", "Paris", "departureDate", "2026-09-10",
                                "returnOrArrivalDate", "2026-09-20")));
        when(validationPersistence.persist(eq(11L), any(), eq("INVALID"), any()))
                .thenAnswer(invocation -> {
                    document.setExtractedJson(invocation.getArgument(1));
                    document.setStatus("INVALID");
                    document.setValidationMessage(invocation.getArgument(3));
                    return document;
                });

        assertThrows(RuntimeException.class, () -> service.validate(7L, "ravi"));

        assertEquals("INVALID", document.getStatus());
        assertEquals("Other Person", new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(document.getExtractedJson()).get("extractedFields").get("passenger").asText());
        assertEquals("DOC_COUNTRY_MISMATCH, DOC_TRAVELLER_MISMATCH", document.getValidationMessage());
        verify(validationPersistence).persist(eq(11L), any(), eq("INVALID"), any());
    }

    @Test
    void matchingFieldsBecomeValid() throws Exception {
        Document document = uploadedDocument();
        when(userService.findByUsername("ravi")).thenReturn(user());
        when(quotationRepository.findByIdAndCreatedByUserId(7L, 1L)).thenReturn(Optional.of(quotation()));
        when(documentRepository.findByQuotationIdOrderByUploadedAtAsc(7L)).thenReturn(List.of(document));
        when(documentStorage.retrieve("stored-ticket")).thenReturn(new byte[] { 1 });
        Map<String, Object> fields = Map.of("passenger", "Ravi Kumar", "origin", "Colombo",
                "destinationCountry", "FR", "destinationCityOrAirport", "Paris",
                "departureDate", "2026-09-10", "returnOrArrivalDate", "2026-09-20");
        when(documentAiClient.extract(any(), any(), any(), any()))
                .thenReturn(new DocumentAiExtraction("TRAVEL_TICKET", fields));
        when(validationPersistence.persist(any(), any(), eq("VALID"), eq(null))).thenAnswer(invocation -> {
            document.setStatus("VALID");
            document.setExtractedJson(invocation.getArgument(1));
            return document;
        });

        assertEquals("VALID", service.validate(7L, "ravi").get(0).status());
    }

    @Test
    void detectedHotelBookingForTicketIsInvalidBeforeBusinessValidation() throws Exception {
        Document document = uploadedDocument();
        when(userService.findByUsername("ravi")).thenReturn(user());
        when(quotationRepository.findByIdAndCreatedByUserId(7L, 1L)).thenReturn(Optional.of(quotation()));
        when(documentRepository.findByQuotationIdOrderByUploadedAtAsc(7L)).thenReturn(List.of(document));
        when(documentStorage.retrieve("stored-ticket")).thenReturn(new byte[] { 1 });
        when(documentAiClient.extract(any(), any(), any(), any())).thenReturn(
                new DocumentAiExtraction("HOTEL_BOOKING", Map.of("guest", "Ravi Kumar")));
        when(validationPersistence.persist(any(), any(), eq("INVALID"), eq("DOC_TYPE_MISMATCH")))
                .thenAnswer(invocation -> {
                    document.setStatus("INVALID");
                    document.setValidationMessage("DOC_TYPE_MISMATCH");
                    return document;
                });

        assertThrows(RuntimeException.class, () -> service.validate(7L, "ravi"));
        assertEquals("DOC_TYPE_MISMATCH", document.getValidationMessage());
    }

    @Test
    void detectedUnknownForHotelBookingIsInvalidBeforeBusinessValidation() throws Exception {
        Document document = uploadedDocument();
        document.setDocumentType("HOTEL_BOOKING");
        when(userService.findByUsername("ravi")).thenReturn(user());
        when(quotationRepository.findByIdAndCreatedByUserId(7L, 1L)).thenReturn(Optional.of(quotation()));
        when(documentRepository.findByQuotationIdOrderByUploadedAtAsc(7L)).thenReturn(List.of(document));
        when(documentStorage.retrieve("stored-ticket")).thenReturn(new byte[] { 1 });
        when(documentAiClient.extract(any(), any(), any(), any())).thenReturn(
                new DocumentAiExtraction("UNKNOWN", Map.of()));
        when(validationPersistence.persist(any(), any(), eq("INVALID"), eq("DOC_TYPE_MISMATCH")))
                .thenAnswer(invocation -> {
                    document.setStatus("INVALID");
                    document.setValidationMessage("DOC_TYPE_MISMATCH");
                    return document;
                });

        assertThrows(RuntimeException.class, () -> service.validate(7L, "ravi"));
        assertEquals("DOC_TYPE_MISMATCH", document.getValidationMessage());
    }

    @Test
    void detectedTravelTicketForHotelBookingIsInvalid() throws Exception {
        Document document = uploadedDocument();
        document.setDocumentType("HOTEL_BOOKING");
        when(userService.findByUsername("ravi")).thenReturn(user());
        when(quotationRepository.findByIdAndCreatedByUserId(7L, 1L)).thenReturn(Optional.of(quotation()));
        when(documentRepository.findByQuotationIdOrderByUploadedAtAsc(7L)).thenReturn(List.of(document));
        when(documentStorage.retrieve("stored-ticket")).thenReturn(new byte[] { 1 });
        when(documentAiClient.extract(any(), any(), any(), any())).thenReturn(
                new DocumentAiExtraction("TRAVEL_TICKET", Map.of()));
        when(validationPersistence.persist(any(), any(), eq("INVALID"), eq("DOC_TYPE_MISMATCH")))
                .thenAnswer(invocation -> {
                    document.setStatus("INVALID");
                    document.setValidationMessage("DOC_TYPE_MISMATCH");
                    return document;
                });

        assertThrows(RuntimeException.class, () -> service.validate(7L, "ravi"));
        assertEquals("INVALID", document.getStatus());
    }

    @Test
    void correctTypeWithDateMismatchUsesDateError() throws Exception {
        Document document = uploadedDocument();
        when(userService.findByUsername("ravi")).thenReturn(user());
        when(quotationRepository.findByIdAndCreatedByUserId(7L, 1L)).thenReturn(Optional.of(quotation()));
        when(documentRepository.findByQuotationIdOrderByUploadedAtAsc(7L)).thenReturn(List.of(document));
        when(documentStorage.retrieve("stored-ticket")).thenReturn(new byte[] { 1 });
        Map<String, Object> fields = Map.of("passenger", "Ravi Kumar", "origin", "Colombo",
                "destinationCountry", "FR", "destinationCityOrAirport", "Paris",
                "departureDate", "2027-09-10", "returnOrArrivalDate", "2027-09-20");
        when(documentAiClient.extract(any(), any(), any(), any()))
                .thenReturn(new DocumentAiExtraction("TRAVEL_TICKET", fields));
        when(validationPersistence.persist(any(), any(), eq("INVALID"), eq("DOC_DATE_MISMATCH")))
                .thenAnswer(invocation -> {
                    document.setStatus("INVALID");
                    document.setValidationMessage("DOC_DATE_MISMATCH");
                    return document;
                });

        assertThrows(RuntimeException.class, () -> service.validate(7L, "ravi"));
        assertEquals("DOC_DATE_MISMATCH", document.getValidationMessage());
    }

    private User user() {
        User user = new User();
        user.setId(1L);
        user.setUsername("ravi");
        return user;
    }

    private Quotation quotation() {
        Quotation quotation = new Quotation();
        quotation.setId(7L);
        quotation.setTravellerName("Ravi Kumar");
        quotation.setDestinationCountry("FR");
        quotation.setDestinationCity("Paris");
        quotation.setTravelStartDate(LocalDate.of(2026, 9, 10));
        quotation.setTravelEndDate(LocalDate.of(2026, 9, 20));
        quotation.setStatus("PAYMENT_CONFIRMED");
        return quotation;
    }

    private Document uploadedDocument() {
        Document document = new Document();
        document.setId(11L);
        document.setQuotationId(7L);
        document.setDocumentType("TRAVEL_TICKET");
        document.setFileName("ticket.png");
        document.setContentType("image/png");
        document.setStorageUri("stored-ticket");
        document.setStatus("UPLOADED");
        return document;
    }
}