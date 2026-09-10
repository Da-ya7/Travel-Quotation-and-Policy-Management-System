package com.company.travel.document.repository;

import com.company.travel.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByQuotationIdOrderByUploadedAtAsc(Long quotationId);
}