package com.company.travel.policy.service;

import com.company.travel.auth.entity.User;
import com.company.travel.auth.entity.UserGroup;
import com.company.travel.auth.exception.ResourceNotFoundException;
import com.company.travel.auth.repository.UserGroupRepository;
import com.company.travel.auth.service.UserService;
import com.company.travel.policy.dto.PolicyReferralResponse;
import com.company.travel.policy.entity.Policy;
import com.company.travel.policy.entity.PolicyReferral;
import com.company.travel.policy.exception.PolicyReferralAccessDeniedException;
import com.company.travel.policy.exception.PolicyReferralException;
import com.company.travel.policy.repository.PolicyReferralRepository;
import com.company.travel.policy.repository.PolicyRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PolicyReferralService {

    private static final String APPROVER_GROUP = "APPROVER_USER";

    private final PolicyReferralRepository referralRepository;
    private final PolicyRepository policyRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserService userService;

    public PolicyReferralService(PolicyReferralRepository referralRepository,
            PolicyRepository policyRepository, UserGroupRepository userGroupRepository,
            UserService userService) {
        this.referralRepository = referralRepository;
        this.policyRepository = policyRepository;
        this.userGroupRepository = userGroupRepository;
        this.userService = userService;
    }

    @Transactional
    public PolicyReferral createPendingReferral(Policy policy, Long referredByUserId,
            LocalDateTime referredAt) {
        UserGroup approverGroup = approverGroup();
        PolicyReferral referral = new PolicyReferral();
        referral.setPolicyId(policy.getId());
        referral.setReferredToGroupId(approverGroup.getId());
        referral.setAssignedApproverUserId(null);
        referral.setReferredByUserId(referredByUserId);
        referral.setReferralStatus(PolicyReferral.ReferralStatus.PENDING);
        referral.setDecidedByUserId(null);
        referral.setDecisionReason(null);
        referral.setReferredAt(referredAt);
        referral.setDecidedAt(null);
        return referralRepository.save(referral);
    }

    @Transactional(readOnly = true)
    public List<PolicyReferralResponse> inbox(String username) {
        User user = authenticatedUser(username);
        return referralRepository.findPendingInbox(user.getId());
    }

    @Transactional
    public PolicyReferralResponse approve(Long policyId, String username) {
        User approver = authenticatedUser(username);
        DecisionContext context = lockedContext(policyId, approver);
        validatePendingApproval(context.policy(), context.referral());

        LocalDateTime now = LocalDateTime.now();
        Policy policy = context.policy();
        PolicyReferral referral = context.referral();
        policy.setStatus("ISSUED");
        policy.setIssuedAt(now);
        policy.setUpdatedAt(now);
        referral.setReferralStatus(PolicyReferral.ReferralStatus.APPROVED);
        referral.setDecidedByUserId(approver.getId());
        referral.setDecisionReason(null);
        referral.setDecidedAt(now);
        policyRepository.save(policy);
        referralRepository.save(referral);
        return PolicyReferralResponse.from(policy, referral);
    }

    @Transactional
    public PolicyReferralResponse reject(Long policyId, String username, String reason) {
        String normalizedReason = reason == null ? null : reason.trim();
        if (normalizedReason == null || normalizedReason.isEmpty()
                || normalizedReason.length() > 500) {
            throw new IllegalArgumentException("reason must be 1 to 500 characters");
        }

        User approver = authenticatedUser(username);
        DecisionContext context = lockedContext(policyId, approver);
        validatePendingApproval(context.policy(), context.referral());

        LocalDateTime now = LocalDateTime.now();
        Policy policy = context.policy();
        PolicyReferral referral = context.referral();
        policy.setStatus("REJECTED");
        policy.setUpdatedAt(now);
        referral.setReferralStatus(PolicyReferral.ReferralStatus.REJECTED);
        referral.setDecidedByUserId(approver.getId());
        referral.setDecisionReason(normalizedReason);
        referral.setDecidedAt(now);
        policyRepository.save(policy);
        referralRepository.save(referral);
        return PolicyReferralResponse.from(policy, referral);
    }

    private DecisionContext lockedContext(Long policyId, User approver) {
        PolicyReferral referral = referralRepository.findByPolicyIdForUpdate(policyId)
                .orElseThrow(() -> new PolicyReferralException(
                        "POLICY_NOT_REFERRED", "Policy has no referral"));
        UserGroup approverGroup = approverGroup();
        if (!approverGroup.getId().equals(referral.getReferredToGroupId())
                || (referral.getAssignedApproverUserId() != null
                        && !referral.getAssignedApproverUserId().equals(approver.getId()))) {
            throw new PolicyReferralAccessDeniedException(
                    "Policy referral is not available to this approver");
        }
        Policy policy = policyRepository.findByIdForUpdate(policyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "POLICY_NOT_FOUND", "Policy not found: " + policyId));
        return new DecisionContext(policy, referral);
    }

    private void validatePendingApproval(Policy policy, PolicyReferral referral) {
        if (referral.getReferralStatus() != PolicyReferral.ReferralStatus.PENDING) {
            throw new PolicyReferralException("REFERRAL_NOT_PENDING",
                    "Policy referral is no longer pending");
        }
        if (!policy.isRequiresApproval() || !"PENDING_APPROVAL".equals(policy.getStatus())) {
            throw new PolicyReferralException("POLICY_NOT_PENDING_APPROVAL",
                    "Policy is not pending approval");
        }
    }

    private UserGroup approverGroup() {
        return userGroupRepository.findByGroupCode(APPROVER_GROUP)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "GROUP_NOT_FOUND", "Approver group not found"));
    }

    private User authenticatedUser(String username) {
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("USER_NOT_FOUND", "User not found: " + username);
        }
        return user;
    }

    private record DecisionContext(Policy policy, PolicyReferral referral) {
    }
}