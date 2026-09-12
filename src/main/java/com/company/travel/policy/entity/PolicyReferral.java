package com.company.travel.policy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "policy_referral")
public class PolicyReferral {

    public enum ReferralStatus {
        PENDING, APPROVED, REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_id", nullable = false, unique = true)
    private Long policyId;

    @Column(name = "referred_to_group_id", nullable = false)
    private Long referredToGroupId;

    @Column(name = "assigned_approver_user_id")
    private Long assignedApproverUserId;

    @Column(name = "referred_by_user_id", nullable = false)
    private Long referredByUserId;

    @Column(name = "referral_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ReferralStatus referralStatus;

    @Column(name = "decided_by_user_id")
    private Long decidedByUserId;

    @Column(name = "decision_reason", length = 500)
    private String decisionReason;

    @Column(name = "referred_at", nullable = false)
    private LocalDateTime referredAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Long policyId) {
        this.policyId = policyId;
    }

    public Long getReferredToGroupId() {
        return referredToGroupId;
    }

    public void setReferredToGroupId(Long referredToGroupId) {
        this.referredToGroupId = referredToGroupId;
    }

    public Long getAssignedApproverUserId() {
        return assignedApproverUserId;
    }

    public void setAssignedApproverUserId(Long assignedApproverUserId) {
        this.assignedApproverUserId = assignedApproverUserId;
    }

    public Long getReferredByUserId() {
        return referredByUserId;
    }

    public void setReferredByUserId(Long referredByUserId) {
        this.referredByUserId = referredByUserId;
    }

    public ReferralStatus getReferralStatus() {
        return referralStatus;
    }

    public void setReferralStatus(ReferralStatus referralStatus) {
        this.referralStatus = referralStatus;
    }

    public Long getDecidedByUserId() {
        return decidedByUserId;
    }

    public void setDecidedByUserId(Long decidedByUserId) {
        this.decidedByUserId = decidedByUserId;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }

    public LocalDateTime getReferredAt() {
        return referredAt;
    }

    public void setReferredAt(LocalDateTime referredAt) {
        this.referredAt = referredAt;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }
}