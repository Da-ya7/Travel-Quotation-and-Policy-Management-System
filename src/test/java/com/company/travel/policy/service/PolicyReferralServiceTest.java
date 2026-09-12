package com.company.travel.policy.service;

import com.company.travel.auth.entity.User;
import com.company.travel.auth.entity.UserGroup;
import com.company.travel.auth.repository.UserGroupRepository;
import com.company.travel.auth.service.UserService;
import com.company.travel.policy.entity.Policy;
import com.company.travel.policy.entity.PolicyReferral;
import com.company.travel.policy.exception.PolicyReferralAccessDeniedException;
import com.company.travel.policy.exception.PolicyReferralException;
import com.company.travel.policy.repository.PolicyReferralRepository;
import com.company.travel.policy.repository.PolicyRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PolicyReferralServiceTest {

    private PolicyReferralRepository referralRepository;
    private PolicyRepository policyRepository;
    private UserGroupRepository userGroupRepository;
    private UserService userService;
    private PolicyReferralService service;
    private UserGroup approverGroup;
    private User approver;

    @BeforeEach
    void setUp() {
        referralRepository = mock(PolicyReferralRepository.class);
        policyRepository = mock(PolicyRepository.class);
        userGroupRepository = mock(UserGroupRepository.class);
        userService = mock(UserService.class);
        service = new PolicyReferralService(referralRepository, policyRepository,
                userGroupRepository, userService);
        approverGroup = new UserGroup();
        approverGroup.setId(20L);
        approverGroup.setGroupCode("APPROVER_USER");
        approver = user(9L, "approver");
        when(userGroupRepository.findByGroupCode("APPROVER_USER"))
                .thenReturn(Optional.of(approverGroup));
        when(userService.findByUsername("approver")).thenReturn(approver);
    }

    @Test
    void createsPendingUnassignedReferralForApproverGroup() {
        Policy policy = policy("PENDING_APPROVAL", true);
        policy.setId(40L);
        when(referralRepository.save(any(PolicyReferral.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PolicyReferral referral = service.createPendingReferral(policy, 7L,
                java.time.LocalDateTime.now());

        assertEquals(40L, referral.getPolicyId());
        assertEquals(20L, referral.getReferredToGroupId());
        assertEquals(7L, referral.getReferredByUserId());
        assertEquals(PolicyReferral.ReferralStatus.PENDING, referral.getReferralStatus());
        assertTrue(referral.getAssignedApproverUserId() == null);
        assertTrue(referral.getReferredAt() != null);
    }

    @Test
    void approverCanApprovePendingReferral() {
        Policy policy = policy("PENDING_APPROVAL", true);
        policy.setId(40L);
        PolicyReferral referral = referral(40L, 20L, null,
                PolicyReferral.ReferralStatus.PENDING);
        when(referralRepository.findByPolicyIdForUpdate(40L)).thenReturn(Optional.of(referral));
        when(policyRepository.findByIdForUpdate(40L)).thenReturn(Optional.of(policy));
        when(policyRepository.save(any(Policy.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(referralRepository.save(any(PolicyReferral.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.approve(40L, "approver");

        assertEquals("ISSUED", policy.getStatus());
        assertEquals(PolicyReferral.ReferralStatus.APPROVED, referral.getReferralStatus());
        assertEquals(9L, referral.getDecidedByUserId());
        assertTrue(policy.getIssuedAt() != null);
        assertEquals(PolicyReferral.ReferralStatus.APPROVED, response.getReferralStatus());
    }

    @Test
    void wrongAssignedApproverIsDenied() {
        Policy policy = policy("PENDING_APPROVAL", true);
        PolicyReferral referral = referral(40L, 20L, 77L,
                PolicyReferral.ReferralStatus.PENDING);
        when(referralRepository.findByPolicyIdForUpdate(40L)).thenReturn(Optional.of(referral));

        assertThrows(PolicyReferralAccessDeniedException.class,
                () -> service.approve(40L, "approver"));
    }

    @Test
    void alreadyDecidedReferralReturnsConflictCode() {
        Policy policy = policy("PENDING_APPROVAL", true);
        PolicyReferral referral = referral(40L, 20L, null,
                PolicyReferral.ReferralStatus.APPROVED);
        when(referralRepository.findByPolicyIdForUpdate(40L)).thenReturn(Optional.of(referral));
        when(policyRepository.findByIdForUpdate(40L)).thenReturn(Optional.of(policy));

        PolicyReferralException exception = assertThrows(PolicyReferralException.class,
                () -> service.approve(40L, "approver"));

        assertEquals("REFERRAL_NOT_PENDING", exception.getErrorCode());
    }

    @Test
    void rejectTrimsAndStoresReason() {
        Policy policy = policy("PENDING_APPROVAL", true);
        PolicyReferral referral = referral(40L, 20L, null,
                PolicyReferral.ReferralStatus.PENDING);
        when(referralRepository.findByPolicyIdForUpdate(40L)).thenReturn(Optional.of(referral));
        when(policyRepository.findByIdForUpdate(40L)).thenReturn(Optional.of(policy));
        when(policyRepository.save(any(Policy.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(referralRepository.save(any(PolicyReferral.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.reject(40L, "approver", "  insufficient documents  ");

        assertEquals("REJECTED", policy.getStatus());
        assertEquals("insufficient documents", referral.getDecisionReason());
        assertEquals(PolicyReferral.ReferralStatus.REJECTED, referral.getReferralStatus());
    }

    @Test
    void blankOrOversizedReasonIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.reject(40L, "approver", "   "));
        assertThrows(IllegalArgumentException.class,
                () -> service.reject(40L, "approver", "x".repeat(501)));
    }

    private Policy policy(String status, boolean requiresApproval) {
        Policy policy = new Policy();
        policy.setStatus(status);
        policy.setRequiresApproval(requiresApproval);
        return policy;
    }

    private PolicyReferral referral(Long policyId, Long groupId, Long assigned,
            PolicyReferral.ReferralStatus status) {
        PolicyReferral referral = new PolicyReferral();
        referral.setPolicyId(policyId);
        referral.setReferredToGroupId(groupId);
        referral.setAssignedApproverUserId(assigned);
        referral.setReferralStatus(status);
        return referral;
    }

    private User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }
}
