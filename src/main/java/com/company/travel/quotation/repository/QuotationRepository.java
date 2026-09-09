package com.company.travel.quotation.repository;

import com.company.travel.quotation.entity.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {

    List<Quotation> findByCreatedByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Quotation> findByIdAndCreatedByUserId(Long id, Long userId);

    boolean existsByQuotationNumber(String quotationNumber);
}