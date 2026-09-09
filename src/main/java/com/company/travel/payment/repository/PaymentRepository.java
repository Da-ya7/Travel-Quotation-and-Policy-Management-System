package com.company.travel.payment.repository;

import com.company.travel.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByQuotationId(Long quotationId);
}
