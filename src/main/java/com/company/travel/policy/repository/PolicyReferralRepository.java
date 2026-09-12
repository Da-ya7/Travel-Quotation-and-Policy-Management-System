package com.company.travel.policy.repository;

import com.company.travel.policy.dto.PolicyReferralResponse;
import com.company.travel.policy.entity.PolicyReferral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface PolicyReferralRepository extends JpaRepository<PolicyReferral, Long> {

    Optional<PolicyReferral> findByPolicyId(Long policyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PolicyReferral r where r.policyId = :policyId")
    Optional<PolicyReferral> findByPolicyIdForUpdate(@Param("policyId") Long policyId);

    @Query("select new com.company.travel.policy.dto.PolicyReferralResponse(" +
            "p.id, p.policyNumber, p.status, p.requiresApproval, p.quotationId, " +
            "r.referralStatus, r.referredAt, r.assignedApproverUserId, r.referredByUserId) " +
            "from PolicyReferral r join Policy p on p.id = r.policyId " +
            "join UserGroup g on g.id = r.referredToGroupId " +
            "where p.requiresApproval = true " +
            "and r.referralStatus = com.company.travel.policy.entity.PolicyReferral$ReferralStatus.PENDING " +
            "and g.groupCode = 'APPROVER_USER' " +
            "and (r.assignedApproverUserId is null or r.assignedApproverUserId = :userId)")
    List<PolicyReferralResponse> findPendingInbox(@Param("userId") Long userId);
}