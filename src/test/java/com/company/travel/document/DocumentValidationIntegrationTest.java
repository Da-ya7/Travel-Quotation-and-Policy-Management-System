package com.company.travel.document;

import com.company.travel.auth.entity.User;
import com.company.travel.auth.repository.UserRepository;
import com.company.travel.document.ai.DocumentAiClient;
import com.company.travel.document.ai.DocumentAiExtraction;
import com.company.travel.document.entity.Document;
import com.company.travel.document.repository.DocumentRepository;
import com.company.travel.document.storage.DocumentStorage;
import com.company.travel.quotation.entity.Quotation;
import com.company.travel.quotation.repository.QuotationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
class DocumentValidationIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private QuotationRepository quotationRepository;
    @Autowired
    private DocumentRepository documentRepository;

    @MockitoBean
    private DocumentAiClient documentAiClient;
    @MockitoBean
    private DocumentStorage documentStorage;

    @Test
    @WithMockUser(username = "phase8-ravi", authorities = "DOCUMENT_UPLOAD")
    void invalidValidationCommitsDocumentStateAndLeavesQuotationUnchanged() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setUsername("phase8-ravi");
        user.setEmail("phase8-" + suffix + "@company.com");
        user.setPasswordHash("test-hash");
        user.setFullName("Ravi Kumar");
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.saveAndFlush(user);

        Quotation quotation = new Quotation();
        quotation.setQuotationNumber("QT-2026-" + suffix);
        quotation.setTravellerName("Ravi Kumar");
        quotation.setTravellerDateOfBirth(LocalDate.of(1990, 1, 1));
        quotation.setPassportNumber("P1234567");
        quotation.setOriginCountry("LK");
        quotation.setDestinationCountry("FR");
        quotation.setDestinationCity("Paris");
        quotation.setTravelStartDate(LocalDate.of(2026, 9, 10));
        quotation.setTravelEndDate(LocalDate.of(2026, 9, 20));
        quotation.setCoverType("TRAVEL");
        quotation.setSumInsured(new BigDecimal("10000"));
        quotation.setCurrency("USD");
        quotation.setStatus("PAYMENT_CONFIRMED");
        quotation.setCreatedByUserId(user.getId());
        quotation.setCreatedAt(LocalDateTime.now());
        quotation.setUpdatedAt(LocalDateTime.now());
        quotation = quotationRepository.saveAndFlush(quotation);

        Document document = new Document();
        document.setQuotationId(quotation.getId());
        document.setDocumentType("TRAVEL_TICKET");
        document.setFileName("ticket.png");
        document.setContentType("image/png");
        document.setStorageUri("phase8-ticket-" + suffix);
        document.setStatus("UPLOADED");
        document.setUploadedByUserId(user.getId());
        document.setUploadedAt(LocalDateTime.now());
        document = documentRepository.saveAndFlush(document);

        when(documentStorage.retrieve(any())).thenReturn(new byte[] { 1 });
        when(documentAiClient.extract(any(), any(), any(), any())).thenReturn(new DocumentAiExtraction("TRAVEL_TICKET",
                Map.of(
                        "passenger", "Ravi Kumar", "origin", "Colombo", "destinationCountry", "LK",
                        "destinationCityOrAirport", "Paris", "departureDate", "2026-09-10",
                        "returnOrArrivalDate", "2026-09-20")));

        mockMvc.perform(post("/api/v1/quotations/" + quotation.getId() + "/documents/validate"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("INVALID_DOCUMENTS_UPLOADED"));

        Document saved = documentRepository.findById(document.getId()).orElseThrow();
        Quotation savedQuotation = quotationRepository.findById(quotation.getId()).orElseThrow();
        assertEquals("INVALID", saved.getStatus());
        assertTrue(saved.getExtractedJson().contains("Ravi Kumar"));
        assertEquals("DOC_COUNTRY_MISMATCH", saved.getValidationMessage());
        assertEquals("PAYMENT_CONFIRMED", savedQuotation.getStatus());
    }
}