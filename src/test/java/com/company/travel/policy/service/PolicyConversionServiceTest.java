package com.company.travel.policy.service;

import com.company.travel.auth.entity.User;
import com.company.travel.auth.exception.ResourceNotFoundException;
import com.company.travel.auth.service.UserService;
import com.company.travel.document.entity.Document;
import com.company.travel.document.repository.DocumentRepository;
import com.company.travel.payment.entity.Payment;
import com.company.travel.payment.repository.PaymentRepository;
import com.company.travel.policy.entity.Policy;
import com.company.travel.policy.entity.PolicyReferral;
import com.company.travel.policy.exception.PolicyConversionException;
import com.company.travel.policy.repository.PolicyRepository;
import com.company.travel.quotation.entity.Quotation;
import com.company.travel.quotation.repository.QuotationRepository;
import com.company.travel.war.service.WarGeographyMatchingService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PolicyConversionServiceTest {

    private QuotationRepository quotationRepository;
    private PaymentRepository paymentRepository;
    private DocumentRepository documentRepository;
    private PolicyRepository policyRepository;
    private UserService userService;
    private WarGeographyMatchingService warGeographyMatchingService;
    private PolicyReferralService policyReferralService;
    private PolicyConversionService service;

    @BeforeEach
    void setUp() {
        quotationRepository = mock(QuotationRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        documentRepository = mock(DocumentRepository.class);
        policyRepository = mock(PolicyRepository.class);
        userService = mock(UserService.class);
        warGeographyMatchingService = mock(WarGeographyMatchingService.class);
        policyReferralService = mock(PolicyReferralService.class);
        when(warGeographyMatchingService.match(any(), any()))
                .thenReturn(new WarGeographyMatchingService.MatchResult(false, null));
        service = new PolicyConversionService(quotationRepository, paymentRepository,
                documentRepository, policyRepository, userService, warGeographyMatchingService,
                policyReferralService);
    }

    @Test
    void convertsOwnEligibleQuotationAndSetsIssuedPolicy() {
        Quotation quotation = givenEligibleQuotation();
        when(policyRepository.existsByPolicyNumber(any())).thenReturn(false);
        when(policyRepository.save(any(Policy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.convert(42L, "uw.ravi");

        assertEquals("ISSUED", response.getStatus());
        assertEquals(42L, response.getQuotationId());
        assertEquals("CONVERTED", quotation.getStatus());
        assertEquals(7L, response.getCreatedByUserId());
        verify(policyRepository).save(any(Policy.class));
        verify(quotationRepository).save(quotation);
        verify(policyReferralService, never()).createPendingReferral(any(), any(), any());
    }

    @Test
    void convertsWarQuotationToPendingApprovalPolicy() {
        Quotation quotation = givenEligibleQuotation();
        when(warGeographyMatchingService.match(any(), any()))
                .thenReturn(new WarGeographyMatchingService.MatchResult(true, null));
        when(policyRepository.existsByPolicyNumber(any())).thenReturn(false);
        when(policyRepository.save(any(Policy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.convert(42L, "uw.ravi");

        assertEquals("PENDING_APPROVAL", response.getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(response.isRequiresApproval());
        verify(policyReferralService).createPendingReferral(any(Policy.class),
                org.mockito.ArgumentMatchers.eq(7L), any());
    }

    @Test
    void referralCreationFailurePropagatesBeforeQuotationConversion() {
        Quotation quotation = givenEligibleQuotation();
        when(warGeographyMatchingService.match(any(), any()))
                .thenReturn(new WarGeographyMatchingService.MatchResult(true, null));
        when(policyRepository.existsByPolicyNumber(any())).thenReturn(false);
        when(policyRepository.save(any(Policy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.doThrow(new IllegalStateException("referral failure"))
                .when(policyReferralService).createPendingReferral(any(), any(), any());

        assertThrows(IllegalStateException.class, () -> service.convert(42L, "uw.ravi"));
        assertEquals("PAYMENT_CONFIRMED", quotation.getStatus());
        verify(quotationRepository, never()).save(quotation);
    }

    @Test
    void anotherUsersQuotationRemainsHidden() {
        when(userService.findByUsername("uw.other")).thenReturn(user(8L, "uw.other"));
        when(quotationRepository.findByIdAndCreatedByUserIdForUpdate(42L, 8L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> service.convert(42L, "uw.other"));

        assertEquals("RESOURCE_NOT_OWNED", exception.getErrorCode());
        verify(policyRepository, never()).save(any());
    }

    @Test
    void rejectsWhenPaymentIsNotSuccessful() {
        Quotation quotation = givenQuotation("PAYMENT_CONFIRMED");
        Payment payment = new Payment();
        payment.setOutcome(Payment.Outcome.FAILED);
        when(paymentRepository.findByQuotationId(42L)).thenReturn(Optional.of(payment));

        PolicyConversionException exception = assertThrows(PolicyConversionException.class,
                () -> service.convert(42L, "uw.ravi"));

        assertEquals("PAYMENT_NOT_CONFIRMED", exception.getErrorCode());
        assertEquals("PAYMENT_CONFIRMED", quotation.getStatus());
        verify(policyRepository, never()).save(any());
    }

    @Test
    void rejectsWhenQuotationIsNotPaymentConfirmed() {
        givenQuotation("QUOTED");

        PolicyConversionException exception = assertThrows(PolicyConversionException.class,
                () -> service.convert(42L, "uw.ravi"));

        assertEquals("PAYMENT_NOT_CONFIRMED", exception.getErrorCode());
        verify(paymentRepository, never()).findByQuotationId(42L);
    }

    @Test
    void rejectsMissingInvalidOrUnknownDocuments() {
        givenQuotation("PAYMENT_CONFIRMED");
        givenEligiblePayment();
        when(documentRepository.findByQuotationIdAndStatus(42L, "VALID"))
                .thenReturn(List.of(document("TRAVEL_TICKET"), document("UNKNOWN")));

        PolicyConversionException exception = assertThrows(PolicyConversionException.class,
                () -> service.convert(42L, "uw.ravi"));

        assertEquals("DOCUMENTS_MANDATORY", exception.getErrorCode());
        verify(policyRepository, never()).save(any());
    }

    @Test
    void repeatedConversionIsRejectedWithoutCreatingAnotherPolicy() {
        givenQuotation("CONVERTED");

        PolicyConversionException exception = assertThrows(PolicyConversionException.class,
                () -> service.convert(42L, "uw.ravi"));

        assertEquals("ALREADY_CONVERTED", exception.getErrorCode());
        verify(policyRepository, never()).save(any());
    }

    @Test
    void policyCreationFailureLeavesQuotationUnconverted() {
        Quotation quotation = givenEligibleQuotation();
        when(policyRepository.existsByPolicyNumber(any())).thenReturn(false);
        when(policyRepository.save(any(Policy.class)))
                .thenThrow(new IllegalStateException("database failure"));

        assertThrows(IllegalStateException.class, () -> service.convert(42L, "uw.ravi"));

        assertEquals("PAYMENT_CONFIRMED", quotation.getStatus());
        verify(quotationRepository, never()).save(quotation);
    }

    private Quotation givenEligibleQuotation() {
        Quotation quotation = givenQuotation("PAYMENT_CONFIRMED");
        givenEligiblePayment();
        when(documentRepository.findByQuotationIdAndStatus(42L, "VALID"))
                .thenReturn(List.of(document("TRAVEL_TICKET"), document("HOTEL_BOOKING")));
        return quotation;
    }

    private void givenEligiblePayment() {
        Payment payment = new Payment();
        payment.setOutcome(Payment.Outcome.SUCCESS);
        when(paymentRepository.findByQuotationId(42L)).thenReturn(Optional.of(payment));
    }

    private Quotation givenQuotation(String status) {
        when(userService.findByUsername("uw.ravi")).thenReturn(user(7L, "uw.ravi"));
        Quotation quotation = new Quotation();
        quotation.setId(42L);
        quotation.setCreatedByUserId(7L);
        quotation.setStatus(status);
        when(quotationRepository.findByIdAndCreatedByUserIdForUpdate(42L, 7L))
                .thenReturn(Optional.of(quotation));
        return quotation;
    }

    private Document document(String type) {
        Document document = new Document();
        document.setDocumentType(type);
        document.setStatus("VALID");
        return document;
    }

    private User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }
}