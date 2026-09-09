package com.company.travel.auth;

import com.company.travel.auth.entity.User;
import com.company.travel.auth.entity.UserGroup;
import com.company.travel.auth.repository.AuthorityRepository;
import com.company.travel.auth.repository.UserGroupRepository;
import com.company.travel.auth.repository.UserRepository;
import com.company.travel.auth.service.AuthorityService;
import com.company.travel.auth.service.EffectiveAuthorityService;
import com.company.travel.auth.service.UserGroupMappingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class Phase5SeedIntegrationTest {

    private static final Set<String> REQUIRED_AUTHORITIES = Set.of(
            "USER_GROUP_CREATE",
            "USER_CREATE",
            "USER_MAP_GROUP",
            "QUOTATION_CREATE",
            "QUOTATION_VIEW_OWN",
            "PAYMENT_COLLECT",
            "DOCUMENT_UPLOAD",
            "QUOTATION_CONVERT_POLICY",
            "POLICY_VIEW_OWN",
            "POLICY_VIEW_REFERRED",
            "POLICY_APPROVE_WAR",
            "POLICY_REJECT_WAR",
            "WAR_GEOGRAPHY_MAINTAIN",
            "AUDIT_VIEW");

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthorityService authorityService;

    @Autowired
    private UserGroupMappingService userGroupMappingService;

    @Autowired
    private EffectiveAuthorityService effectiveAuthorityService;

    @Test
    void flywaySeedsExactlyTheRequiredAuthorityCatalogue() {
        assertEquals(REQUIRED_AUTHORITIES, authorityRepository.findAll().stream()
                .map(authority -> authority.getAuthorityCode())
                .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void flywaySeedsGroupsWithExactAuthorityBoundaries() {
        assertEquals(
                Set.of("QUOTATION_CREATE", "QUOTATION_VIEW_OWN", "PAYMENT_COLLECT",
                        "DOCUMENT_UPLOAD", "QUOTATION_CONVERT_POLICY", "POLICY_VIEW_OWN"),
                authorityCodesFor("UNDERWRITING_USER"));
        assertEquals(
                Set.of("POLICY_VIEW_REFERRED", "POLICY_APPROVE_WAR", "POLICY_REJECT_WAR"),
                authorityCodesFor("APPROVER_USER"));
        assertEquals(
                Set.of("USER_GROUP_CREATE", "USER_CREATE", "USER_MAP_GROUP",
                        "WAR_GEOGRAPHY_MAINTAIN", "AUDIT_VIEW"),
                authorityCodesFor("SYSTEM_ADMIN"));
    }

    @Test
    void effectiveAuthoritiesComeFromEachMappedSeededGroup() {
        assertEffectiveAuthorities("UNDERWRITING_USER", "underwriting",
                "USER_CREATE", "POLICY_APPROVE_WAR");
        assertEffectiveAuthorities("APPROVER_USER", "approver",
                "QUOTATION_CREATE", "USER_CREATE");
        assertEffectiveAuthorities("SYSTEM_ADMIN", "system-admin",
                "QUOTATION_CREATE", "PAYMENT_COLLECT", "DOCUMENT_UPLOAD");
    }

    private void assertEffectiveAuthorities(
            String groupCode,
            String usernameSuffix,
            String... forbiddenAuthorities) {

        UserGroup group = userGroupRepository.findByGroupCode(groupCode).orElseThrow();
        User user = new User();
        user.setUsername("phase5-" + usernameSuffix);
        user.setEmail("phase5-" + usernameSuffix + "@company.com");
        user.setPasswordHash("test-only-hash");
        user.setFullName("Phase 5 " + usernameSuffix);
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        userGroupMappingService.mapUserToGroup(user.getId(), group.getId(), LocalDate.now());

        Set<String> effectiveAuthorities = effectiveAuthorityService
                .getEffectiveAuthorities(user.getId());

        assertEquals(authorityCodesFor(groupCode), effectiveAuthorities);
        for (String forbiddenAuthority : forbiddenAuthorities) {
            assertFalse(effectiveAuthorities.contains(forbiddenAuthority));
        }
    }

    private Set<String> authorityCodesFor(String groupCode) {
        UserGroup group = userGroupRepository.findByGroupCode(groupCode).orElseThrow();
        assertNotNull(group.getId());

        return authorityService.getAuthoritiesForGroup(group.getId()).stream()
                .collect(java.util.stream.Collectors.toSet());
    }
}