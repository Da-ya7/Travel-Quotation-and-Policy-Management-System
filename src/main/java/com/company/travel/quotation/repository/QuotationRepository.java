package com.company.travel.quotation.repository;

import com.company.travel.quotation.entity.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {

    List<Quotation> findByCreatedByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Quotation> findByIdAndCreatedByUserId(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Quotation q where q.id = :id and q.createdByUserId = :userId")
    Optional<Quotation> findByIdAndCreatedByUserIdForUpdate(
            @Param("id") Long id, @Param("userId") Long userId);

    boolean existsByQuotationNumber(String quotationNumber);
}