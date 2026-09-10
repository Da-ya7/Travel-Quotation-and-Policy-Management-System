package com.company.travel.document.service;

import com.company.travel.document.entity.Document;
import com.company.travel.document.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentValidationPersistence {

    private final DocumentRepository documentRepository;

    public DocumentValidationPersistence(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Document persist(Long documentId, String extractedJson, String status, String validationMessage) {
        Document document = documentRepository.findById(documentId).orElseThrow();
        document.setExtractedJson(extractedJson);
        document.setStatus(status);
        document.setValidationMessage(validationMessage);
        return documentRepository.save(document);
    }
}