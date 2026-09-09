package com.company.travel.quotation.controller;

import com.company.travel.quotation.dto.CreateQuotationRequest;
import com.company.travel.quotation.dto.QuotationResponse;
import com.company.travel.quotation.service.QuotationService;

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

    public QuotationController(QuotationService quotationService) {
        this.quotationService = quotationService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('QUOTATION_CREATE')")
    public ResponseEntity<QuotationResponse> create(
            @Valid @RequestBody CreateQuotationRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(quotationService.create(request, authentication.getName()));
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