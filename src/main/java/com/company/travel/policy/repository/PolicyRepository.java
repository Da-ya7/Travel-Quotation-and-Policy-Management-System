package com.company.travel.policy.repository;

import com.company.travel.policy.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    boolean existsByPolicyNumber(String policyNumber);

    Optional<Policy> findByQuotationId(Long quotationId);
}