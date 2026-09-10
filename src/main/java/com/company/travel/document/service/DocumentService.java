package com.company.travel.document.service;

import com.company.travel.auth.entity.User;
import com.company.travel.auth.exception.ResourceNotFoundException;
import com.company.travel.auth.service.UserService;
import com.company.travel.document.ai.DocumentAiClient;
import com.company.travel.document.ai.DocumentAiExtraction;
import com.company.travel.document.dto.DocumentResponse;
import com.company.travel.document.entity.Document;
import com.company.travel.document.exception.InvalidDocumentsException;
import com.company.travel.document.repository.DocumentRepository;
import com.company.travel.document.storage.DocumentStorage;
import com.company.travel.quotation.entity.Quotation;
import com.company.travel.quotation.repository.QuotationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DocumentService {
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final List<String> TYPES = List.of("TRAVEL_TICKET", "HOTEL_BOOKING");
    private static final List<String> CONTENT_TYPES = List.of("application/pdf", "image/jpeg", "image/png");

    private final DocumentRepository documentRepository;
    private final QuotationRepository quotationRepository;
    private final UserService userService;
    private final DocumentStorage documentStorage;
    private final DocumentAiClient documentAiClient;
    private final DocumentValidationPersistence validationPersistence;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final long dateToleranceDays;
    private final double fuzzyThreshold;

    public DocumentService(DocumentRepository documentRepository, QuotationRepository quotationRepository,
            UserService userService, DocumentStorage documentStorage, DocumentAiClient documentAiClient,
            DocumentValidationPersistence validationPersistence,
            @Value("${document.validation.date-tolerance-days:0}") long dateToleranceDays,
            @Value("${document.validation.fuzzy-threshold:0.8}") double fuzzyThreshold) {
        this.documentRepository = documentRepository;
        this.quotationRepository = quotationRepository;
        this.userService = userService;
        this.documentStorage = documentStorage;
        this.documentAiClient = documentAiClient;
        this.validationPersistence = validationPersistence;
        this.dateToleranceDays = dateToleranceDays;
        this.fuzzyThreshold = fuzzyThreshold;
    }

    @Transactional
    public DocumentResponse upload(Long quotationId, MultipartFile file, String documentType, String username) {
        Quotation quotation = ownedQuotation(quotationId, username);
        validateUpload(file, documentType);
        User user = userService.findByUsername(username);
        try {
            String storageUri = documentStorage.store(file.getOriginalFilename(), file.getBytes());
            Document document = new Document();
            document.setQuotationId(quotation.getId());
            document.setDocumentType(documentType);
            document.setFileName(file.getOriginalFilename());
            document.setContentType(file.getContentType());
            document.setStorageUri(storageUri);
            document.setStatus("UPLOADED");
            document.setUploadedByUserId(user.getId());
            document.setUploadedAt(LocalDateTime.now());
            return DocumentResponse.from(documentRepository.save(document));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to store document", exception);
        }
    }

    public List<DocumentResponse> validate(Long quotationId, String username) {
        Quotation quotation = ownedQuotation(quotationId, username);
        List<Document> documents = documentRepository.findByQuotationIdOrderByUploadedAtAsc(quotationId)
                .stream().filter(document -> "UPLOADED".equals(document.getStatus())).toList();
        List<String> failures = new ArrayList<>();
        for (Document document : documents) {
            try {
                DocumentAiExtraction extraction = documentAiClient.extract(
                        documentStorage.retrieve(document.getStorageUri()),
                        document.getFileName(), document.getContentType(), document.getDocumentType());
                String extractedJson = objectMapper.writeValueAsString(extraction);
                List<String> documentFailures = extraction.documentType().equals(document.getDocumentType())
                        ? validateFields(quotation, document.getDocumentType(), extraction.extractedFields())
                        : List.of("DOC_TYPE_MISMATCH");
                String status = documentFailures.isEmpty() ? "VALID" : "INVALID";
                String validationMessage = documentFailures.isEmpty()
                        ? null
                        : String.join(", ", documentFailures);
                Document persisted = validationPersistence.persist(
                        document.getId(), extractedJson, status, validationMessage);
                document.setExtractedJson(persisted.getExtractedJson());
                document.setStatus(persisted.getStatus());
                document.setValidationMessage(persisted.getValidationMessage());
                if (!documentFailures.isEmpty()) {
                    failures.add("document " + document.getId() + ": " + String.join(", ", documentFailures));
                }
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to retrieve or persist document data", exception);
            }
        }
        if (!failures.isEmpty())
            throw new InvalidDocumentsException(failures);
        return documents.stream().map(DocumentResponse::from).toList();
    }

    private Quotation ownedQuotation(Long quotationId, String username) {
        User user = userService.findByUsername(username);
        if (user == null)
            throw new ResourceNotFoundException("USER_NOT_FOUND", "User not found: " + username);
        return quotationRepository.findByIdAndCreatedByUserId(quotationId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("RESOURCE_NOT_OWNED",
                        "Quotation is not owned by the authenticated user"));
    }

    private void validateUpload(MultipartFile file, String documentType) {
        if (!TYPES.contains(documentType))
            throw new IllegalArgumentException(
                    "documentType must be TRAVEL_TICKET or HOTEL_BOOKING");
        if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_SIZE)
            throw new IllegalArgumentException(
                    "Document must be between 1 byte and 10 MB");
        if (!CONTENT_TYPES.contains(file.getContentType()))
            throw new IllegalArgumentException(
                    "Document must be PDF, JPEG, or PNG");
    }

    private List<String> validateFields(Quotation quotation, String documentType, Map<String, Object> fields) {
        List<String> failures = new ArrayList<>();
        if ("TRAVEL_TICKET".equals(documentType)) {
            country(failures, fields.get("destinationCountry"), quotation.getDestinationCountry());
            dates(failures, fields.get("departureDate"), fields.get("returnOrArrivalDate"), quotation);
            location(failures, fields.get("destinationCityOrAirport"), quotation.getDestinationCity());
            traveller(failures, fields.get("passenger"), quotation.getTravellerName());
        } else if ("HOTEL_BOOKING".equals(documentType)) {
            country(failures, fields.get("hotelCountry"), quotation.getDestinationCountry());
            dates(failures, fields.get("checkInDate"), fields.get("checkOutDate"), quotation);
            location(failures, fields.get("hotelCity"), quotation.getDestinationCity());
            traveller(failures, fields.get("guest"), quotation.getTravellerName());
        } else
            failures.add("INVALID_DOCUMENT_TYPE");
        return failures;
    }

    private void country(List<String> failures, Object value, String expected) {
        if (value == null || !expected.equalsIgnoreCase(value.toString().trim()))
            failures.add("DOC_COUNTRY_MISMATCH");
    }

    private void location(List<String> failures, Object value, String expected) {
        if (value != null && !similar(value.toString(), expected))
            failures.add("DOC_LOCATION_MISMATCH");
    }

    private void traveller(List<String> failures, Object value, String expected) {
        if (value == null || similarity(value.toString(), expected) < fuzzyThreshold)
            failures.add("DOC_TRAVELLER_MISMATCH");
    }

    private void dates(List<String> failures, Object startValue, Object endValue, Quotation quotation) {
        try {
            LocalDate start = LocalDate.parse(String.valueOf(startValue));
            LocalDate end = LocalDate.parse(String.valueOf(endValue));
            LocalDate quotationStart = quotation.getTravelStartDate().minusDays(dateToleranceDays);
            LocalDate quotationEnd = quotation.getTravelEndDate().plusDays(dateToleranceDays);
            if (end.isBefore(start) || start.isAfter(quotationEnd) || end.isBefore(quotationStart)) {
                failures.add("DOC_DATE_MISMATCH");
            }
        } catch (RuntimeException exception) {
            failures.add("DOC_DATE_MISMATCH");
        }
    }

    private boolean similar(String actual, String expected) {
        return similarity(actual, expected) >= fuzzyThreshold;
    }

    private double similarity(String first, String second) {
        String left = first.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        String right = second.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (left.equals(right))
            return 1.0;
        int distance = levenshtein(left, right);
        return Math.max(left.length(), right.length()) == 0 ? 1.0
                : 1.0 - ((double) distance / Math.max(left.length(), right.length()));
    }

    private int levenshtein(String first, String second) {
        int[] previous = new int[second.length() + 1];
        for (int index = 0; index <= second.length(); index++)
            previous[index] = index;
        for (int row = 1; row <= first.length(); row++) {
            int[] current = new int[second.length() + 1];
            current[0] = row;
            for (int column = 1; column <= second.length(); column++)
                current[column] = Math.min(
                        Math.min(current[column - 1] + 1, previous[column] + 1),
                        previous[column - 1] + (first.charAt(row - 1) == second.charAt(column - 1) ? 0 : 1));
            previous = current;
        }
        return previous[second.length()];
    }
}