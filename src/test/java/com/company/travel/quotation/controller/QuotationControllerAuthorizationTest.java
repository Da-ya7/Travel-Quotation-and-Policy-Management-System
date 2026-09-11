package com.company.travel.quotation.controller;

import com.company.travel.auth.service.CustomUserDetailsService;
import com.company.travel.auth.service.JwtService;
import com.company.travel.auth.service.UserService;
import com.company.travel.config.SecurityConfig;
import com.company.travel.quotation.dto.QuotationResponse;
import com.company.travel.quotation.service.QuotationService;
import com.company.travel.policy.service.PolicyConversionService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebMvcTest(QuotationController.class)
@Import(SecurityConfig.class)
class QuotationControllerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private QuotationService quotationService;

    @MockitoBean
    private PolicyConversionService policyConversionService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserService userService;

    @Test
    void missingJwtIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/quotations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidJwtIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/quotations")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "uw.ravi", authorities = "OTHER_AUTHORITY")
    void missingCreateAuthorityIsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/quotations")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "uw.ravi", authorities = "QUOTATION_CREATE")
    void createWithAuthoritySucceeds() throws Exception {
        when(quotationService.create(any(), eq("uw.ravi"))).thenReturn(response(9L));

        mockMvc.perform(post("/api/v1/quotations")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @WithMockUser(username = "uw.ravi", authorities = "OTHER_AUTHORITY")
    void missingConvertAuthorityIsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/quotations/9/convert-to-policy"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "uw.ravi", authorities = "QUOTATION_CONVERT_POLICY")
    void convertWithAuthorityCallsService() throws Exception {
        com.company.travel.policy.entity.Policy policy = new com.company.travel.policy.entity.Policy();
        policy.setId(10L);
        policy.setPolicyNumber("PL-2026-000001");
        policy.setQuotationId(9L);
        policy.setStatus("ISSUED");
        when(policyConversionService.convert(9L, "uw.ravi"))
                .thenReturn(com.company.travel.policy.dto.PolicyResponse.from(policy));

        mockMvc.perform(post("/api/v1/quotations/9/convert-to-policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quotationId").value(9))
                .andExpect(jsonPath("$.status").value("ISSUED"));
    }

    @Test
    @WithMockUser(username = "uw.ravi", authorities = "QUOTATION_VIEW_OWN")
    void listWithViewAuthoritySucceeds() throws Exception {
        when(quotationService.findOwn("uw.ravi")).thenReturn(java.util.List.of(response(9L)));

        mockMvc.perform(get("/api/v1/quotations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9));
    }

    @Test
    @WithMockUser(username = "uw.ravi", authorities = "QUOTATION_VIEW_OWN")
    void unownedQuotationReturnsResourceNotOwned() throws Exception {
        when(quotationService.findOwnById(99L, "uw.ravi"))
                .thenThrow(new com.company.travel.auth.exception.ResourceNotFoundException(
                        "RESOURCE_NOT_OWNED", "Quotation is not owned by the authenticated user"));

        mockMvc.perform(get("/api/v1/quotations/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_OWNED"));
    }

    @Test
    @WithMockUser(username = "uw.ravi", authorities = "QUOTATION_CREATE")
    void invalidRequestUsesStandardValidationResponse() throws Exception {
        mockMvc.perform(post("/api/v1/quotations")
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    private java.util.Map<String, Object> validRequest() {
        return java.util.Map.of(
                "travellerName", "Ravi Kumar",
                "travellerDateOfBirth", "1990-01-01",
                "passportNumber", "P1234567",
                "originCountry", "LK",
                "destinationCountry", "FR",
                "destinationCity", "Paris",
                "travelStartDate", "2026-09-10",
                "travelEndDate", "2026-09-20",
                "coverType", "TRAVEL",
                "sumInsured", new BigDecimal("10000"));
    }

    private QuotationResponse response(Long id) {
        return new QuotationResponse(id, "QT-2026-000001", "DRAFT");
    }
}