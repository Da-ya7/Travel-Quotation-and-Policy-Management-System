package com.company.travel.document.dto;

import com.company.travel.document.entity.Document;

import java.time.LocalDateTime;

public record DocumentResponse(Long id, Long quotationId, String documentType, String fileName,
        String contentType, String status, String extractedJson, String validationMessage,
        LocalDateTime uploadedAt) {
    public static DocumentResponse from(Document document) {
        return new DocumentResponse(document.getId(), document.getQuotationId(), document.getDocumentType(),
                document.getFileName(), document.getContentType(), document.getStatus(),
                document.getExtractedJson(), document.getValidationMessage(), document.getUploadedAt());
    }
}