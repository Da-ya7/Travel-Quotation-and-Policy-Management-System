package com.company.travel.policy.dto;

import com.company.travel.policy.entity.Policy;

import java.time.LocalDateTime;

public class PolicyResponse {

    private Long id;
    private String policyNumber;
    private Long quotationId;
    private String status;
    private boolean requiresApproval;
    private Long createdByUserId;
    private LocalDateTime issuedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PolicyResponse from(Policy policy) {
        PolicyResponse response = new PolicyResponse();
        response.id = policy.getId();
        response.policyNumber = policy.getPolicyNumber();
        response.quotationId = policy.getQuotationId();
        response.status = policy.getStatus();
        response.requiresApproval = policy.isRequiresApproval();
        response.createdByUserId = policy.getCreatedByUserId();
        response.issuedAt = policy.getIssuedAt();
        response.createdAt = policy.getCreatedAt();
        response.updatedAt = policy.getUpdatedAt();
        return response;
    }

    public Long getId() {
        return id;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public Long getQuotationId() {
        return quotationId;
    }

    public String getStatus() {
        return status;
    }

    public boolean isRequiresApproval() {
        return requiresApproval;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}