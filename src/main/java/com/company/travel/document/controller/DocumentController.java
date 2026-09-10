package com.company.travel.document.controller;

import com.company.travel.document.dto.DocumentResponse;
import com.company.travel.document.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/quotations/{quotationId}/documents")
public class DocumentController {
    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('DOCUMENT_UPLOAD')")
    public ResponseEntity<DocumentResponse> upload(@PathVariable Long quotationId,
            @RequestParam("document") MultipartFile document, @RequestParam("documentType") String documentType,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.upload(
                quotationId, document, documentType, authentication.getName()));
    }

    @PostMapping("/validate")
    @PreAuthorize("hasAuthority('DOCUMENT_UPLOAD')")
    public ResponseEntity<List<DocumentResponse>> validate(@PathVariable Long quotationId,
            Authentication authentication) {
        return ResponseEntity.ok(documentService.validate(quotationId, authentication.getName()));
    }
}