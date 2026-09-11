package com.company.travel.quotation.controller;

import com.company.travel.quotation.dto.CreateQuotationRequest;
import com.company.travel.quotation.dto.QuotationResponse;
import com.company.travel.quotation.service.QuotationService;
import com.company.travel.policy.dto.PolicyResponse;
import com.company.travel.policy.service.PolicyConversionService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/quotations")
public class QuotationController {

    private final QuotationService quotationService;
    private final PolicyConversionService policyConversionService;

    public QuotationController(QuotationService quotationService,
            PolicyConversionService policyConversionService) {
        this.quotationService = quotationService;
        this.policyConversionService = policyConversionService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('QUOTATION_CREATE')")
    public ResponseEntity<QuotationResponse> create(
            @Valid @RequestBody CreateQuotationRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(quotationService.create(request, authentication.getName()));
    }

    @PostMapping("/{id}/finalize")
    @PreAuthorize("hasAuthority('QUOTATION_CREATE')")
    public ResponseEntity<QuotationResponse> finalizeQuotation(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(
                quotationService.finalizeQuotation(id, authentication.getName()));
    }

    @PostMapping("/{id}/convert-to-policy")
    @PreAuthorize("hasAuthority('QUOTATION_CONVERT_POLICY')")
    public ResponseEntity<PolicyResponse> convertToPolicy(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(
                policyConversionService.convert(id, authentication.getName()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('QUOTATION_VIEW_OWN')")
    public ResponseEntity<List<QuotationResponse>> findOwn(Authentication authentication) {
        return ResponseEntity.ok(quotationService.findOwn(authentication.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('QUOTATION_VIEW_OWN')")
    public ResponseEntity<QuotationResponse> findOwnById(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(
                quotationService.findOwnById(id, authentication.getName()));
    }
}