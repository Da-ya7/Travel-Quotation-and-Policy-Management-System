package com.company.travel.policy.controller;

import com.company.travel.policy.dto.PolicyReferralResponse;
import com.company.travel.policy.dto.RejectPolicyRequest;
import com.company.travel.policy.service.PolicyReferralService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/policies")
public class PolicyController {

    private final PolicyReferralService referralService;

    public PolicyController(PolicyReferralService referralService) {
        this.referralService = referralService;
    }

    @GetMapping("/referred")
    @PreAuthorize("hasAuthority('POLICY_VIEW_REFERRED')")
    public ResponseEntity<List<PolicyReferralResponse>> inbox(Authentication authentication) {
        return ResponseEntity.ok(referralService.inbox(authentication.getName()));
    }

    @PostMapping("/{policyId}/approve")
    @PreAuthorize("hasAuthority('POLICY_APPROVE_WAR')")
    public ResponseEntity<PolicyReferralResponse> approve(
            @PathVariable Long policyId, Authentication authentication) {
        return ResponseEntity.ok(referralService.approve(policyId, authentication.getName()));
    }

    @PostMapping("/{policyId}/reject")
    @PreAuthorize("hasAuthority('POLICY_REJECT_WAR')")
    public ResponseEntity<PolicyReferralResponse> reject(
            @PathVariable Long policyId, @Valid @RequestBody RejectPolicyRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(referralService.reject(
                policyId, authentication.getName(), request.getReason()));
    }
}