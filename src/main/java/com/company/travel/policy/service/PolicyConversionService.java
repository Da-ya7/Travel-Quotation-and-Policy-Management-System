package com.company.travel.policy.service;

import com.company.travel.auth.entity.User;
import com.company.travel.auth.exception.ResourceNotFoundException;
import com.company.travel.auth.service.UserService;
import com.company.travel.document.entity.Document;
import com.company.travel.document.repository.DocumentRepository;
import com.company.travel.payment.entity.Payment;
import com.company.travel.payment.repository.PaymentRepository;
import com.company.travel.policy.dto.PolicyResponse;
import com.company.travel.policy.entity.Policy;
import com.company.travel.policy.exception.PolicyConversionException;
import com.company.travel.policy.repository.PolicyRepository;
import com.company.travel.quotation.entity.Quotation;
import com.company.travel.quotation.repository.QuotationRepository;
import com.company.travel.war.service.WarGeographyMatchingService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PolicyConversionService {

    private static final Set<String> REQUIRED_DOCUMENT_TYPES = Set.of(
            "TRAVEL_TICKET", "HOTEL_BOOKING");

    private final QuotationRepository quotationRepository;
    private final PaymentRepository paymentRepository;
    private final DocumentRepository documentRepository;
    private final PolicyRepository policyRepository;
    private final UserService userService;
    private final WarGeographyMatchingService warGeographyMatchingService;
    private final PolicyReferralService policyReferralService;

    public PolicyConversionService(QuotationRepository quotationRepository,
            PaymentRepository paymentRepository, DocumentRepository documentRepository,
            PolicyRepository policyRepository, UserService userService,
            WarGeographyMatchingService warGeographyMatchingService,
            PolicyReferralService policyReferralService) {
        this.quotationRepository = quotationRepository;
        this.paymentRepository = paymentRepository;
        this.documentRepository = documentRepository;
        this.policyRepository = policyRepository;
        this.userService = userService;
        this.warGeographyMatchingService = warGeographyMatchingService;
        this.policyReferralService = policyReferralService;
    }

    @Transactional
    public PolicyResponse convert(Long quotationId, String username) {
        User user = authenticatedUser(username);
        Quotation quotation = quotationRepository.findByIdAndCreatedByUserIdForUpdate(
                quotationId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RESOURCE_NOT_OWNED",
                        "Quotation is not owned by the authenticated user"));

        if ("CONVERTED".equals(quotation.getStatus())) {
            throw new PolicyConversionException("ALREADY_CONVERTED",
                    "Quotation has already been converted to a policy");
        }
        if (!"PAYMENT_CONFIRMED".equals(quotation.getStatus())) {
            throw new PolicyConversionException("PAYMENT_NOT_CONFIRMED",
                    "Quotation must be in PAYMENT_CONFIRMED status before conversion");
        }

        Payment payment = paymentRepository.findByQuotationId(quotationId).orElse(null);
        if (payment == null || payment.getOutcome() != Payment.Outcome.SUCCESS) {
            throw new PolicyConversionException("PAYMENT_NOT_CONFIRMED",
                    "A successful payment is required before conversion");
        }

        Set<String> validDocumentTypes = new HashSet<>();
        for (Document document : documentRepository.findByQuotationIdAndStatus(quotationId, "VALID")) {
            if (REQUIRED_DOCUMENT_TYPES.contains(document.getDocumentType())) {
                validDocumentTypes.add(document.getDocumentType());
            }
        }
        if (!validDocumentTypes.containsAll(REQUIRED_DOCUMENT_TYPES)) {
            throw new PolicyConversionException("DOCUMENTS_MANDATORY",
                    "A VALID TRAVEL_TICKET and HOTEL_BOOKING document are required before conversion");
        }

        boolean requiresApproval = warGeographyMatchingService.match(
                quotation.getDestinationCountry(), quotation.getDestinationCity()).matched();

        LocalDateTime now = LocalDateTime.now();
        Policy policy = new Policy();
        policy.setPolicyNumber(generatePolicyNumber());
        policy.setQuotationId(quotation.getId());
        policy.setStatus(requiresApproval ? "PENDING_APPROVAL" : "ISSUED");
        policy.setRequiresApproval(requiresApproval);
        policy.setCreatedByUserId(user.getId());
        policy.setIssuedAt(now);
        policy.setCreatedAt(now);
        policy.setUpdatedAt(now);
        Policy savedPolicy = policyRepository.save(policy);

        if (requiresApproval) {
            policyReferralService.createPendingReferral(savedPolicy, user.getId(), now);
        }

        quotation.setStatus("CONVERTED");
        quotation.setUpdatedAt(now);
        quotationRepository.save(quotation);

        return PolicyResponse.from(savedPolicy);
    }

    private User authenticatedUser(String username) {
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("USER_NOT_FOUND", "User not found: " + username);
        }
        return user;
    }

    private String generatePolicyNumber() {
        int year = LocalDate.now().getYear();

        for (int attempt = 0; attempt < 10; attempt++) {
            int sequence = ThreadLocalRandom.current().nextInt(0, 1_000_000);
            String number = String.format("PL-%04d-%06d", year, sequence);

            if (!policyRepository.existsByPolicyNumber(number)) {
                return number;
            }
        }

        throw new IllegalStateException("Unable to generate a unique policy number");
    }
}