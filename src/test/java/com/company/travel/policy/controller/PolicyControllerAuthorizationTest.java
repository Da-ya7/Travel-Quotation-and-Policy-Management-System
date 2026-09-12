package com.company.travel.policy.controller;

import com.company.travel.auth.service.CustomUserDetailsService;
import com.company.travel.auth.service.JwtService;
import com.company.travel.config.SecurityConfig;
import com.company.travel.policy.service.PolicyReferralService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PolicyController.class)
@Import(SecurityConfig.class)
class PolicyControllerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PolicyReferralService referralService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    @WithMockUser(authorities = "POLICY_VIEW_REFERRED")
    void referralInboxAuthorityIsAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/policies/referred"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "QUOTATION_CREATE")
    void wrongInboxAuthorityIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/policies/referred"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedInboxIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/policies/referred"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "POLICY_APPROVE_WAR")
    void approveAuthorityIsRequired() throws Exception {
        mockMvc.perform(post("/api/v1/policies/40/approve"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "POLICY_REJECT_WAR")
    void rejectAuthorityRequiresReasonValidation() throws Exception {
        mockMvc.perform(post("/api/v1/policies/40/reject")
                .contentType("application/json")
                .content("{\"reason\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }
}
