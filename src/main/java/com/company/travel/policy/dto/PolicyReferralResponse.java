package com.company.travel.policy.dto;

import com.company.travel.policy.entity.PolicyReferral;

import java.time.LocalDateTime;

public class PolicyReferralResponse {

    private final Long policyId;
    private final String policyNumber;
    private final String policyStatus;
    private final boolean requiresApproval;
    private final Long quotationId;
    private final PolicyReferral.ReferralStatus referralStatus;
    private final LocalDateTime referredAt;
    private final Long assignedApproverUserId;
    private final Long referredByUserId;

    public PolicyReferralResponse(Long policyId, String policyNumber, String policyStatus,
            boolean requiresApproval, Long quotationId,
            PolicyReferral.ReferralStatus referralStatus, LocalDateTime referredAt,
            Long assignedApproverUserId, Long referredByUserId) {
        this.policyId = policyId;
        this.policyNumber = policyNumber;
        this.policyStatus = policyStatus;
        this.requiresApproval = requiresApproval;
        this.quotationId = quotationId;
        this.referralStatus = referralStatus;
        this.referredAt = referredAt;
        this.assignedApproverUserId = assignedApproverUserId;
        this.referredByUserId = referredByUserId;
    }

    public static PolicyReferralResponse from(com.company.travel.policy.entity.Policy policy,
            PolicyReferral referral) {
        return new PolicyReferralResponse(policy.getId(), policy.getPolicyNumber(),
                policy.getStatus(), policy.isRequiresApproval(), policy.getQuotationId(),
                referral.getReferralStatus(), referral.getReferredAt(),
                referral.getAssignedApproverUserId(), referral.getReferredByUserId());
    }

    public Long getPolicyId() {
        return policyId;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public String getPolicyStatus() {
        return policyStatus;
    }

    public boolean isRequiresApproval() {
        return requiresApproval;
    }

    public Long getQuotationId() {
        return quotationId;
    }

    public PolicyReferral.ReferralStatus getReferralStatus() {
        return referralStatus;
    }

    public LocalDateTime getReferredAt() {
        return referredAt;
    }

    public Long getAssignedApproverUserId() {
        return assignedApproverUserId;
    }

    public Long getReferredByUserId() {
        return referredByUserId;
    }
}