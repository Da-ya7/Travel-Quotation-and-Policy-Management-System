package com.company.travel.war.controller;

import com.company.travel.auth.service.CustomUserDetailsService;
import com.company.travel.auth.service.JwtService;
import com.company.travel.config.SecurityConfig;
import com.company.travel.war.entity.WarGeography;
import com.company.travel.war.exception.DuplicateWarGeographyException;
import com.company.travel.war.service.WarGeographyService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WarGeographyController.class)
@Import(SecurityConfig.class)
class WarGeographyControllerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WarGeographyService service;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    @WithMockUser(authorities = "WAR_GEOGRAPHY_MAINTAIN")
    void maintainAuthorityCanCreateGeography() throws Exception {
        WarGeography geography = new WarGeography();
        geography.setId(1L);
        geography.setCountryCode("US");
        geography.setActive(true);
        when(service.create(any())).thenReturn(geography);

        mockMvc.perform(post("/api/v1/admin/war-geographies")
                .contentType("application/json")
                .content("{\"countryCode\":\"US\",\"active\":true}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = "QUOTATION_CREATE")
    void missingAuthorityIsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/admin/war-geographies")
                .contentType("application/json")
                .content("{\"countryCode\":\"US\",\"active\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/admin/war-geographies")
                .contentType("application/json")
                .content("{\"countryCode\":\"US\",\"active\":true}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "WAR_GEOGRAPHY_MAINTAIN")
    void invalidCountryAndMissingActiveAreRejected() throws Exception {
        mockMvc.perform(post("/api/v1/admin/war-geographies")
                .contentType("application/json")
                .content("{\"countryCode\":\"XXX\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "WAR_GEOGRAPHY_MAINTAIN")
    void invalidCountryAndOversizedRegionAreRejected() throws Exception {
        when(service.create(any())).thenThrow(new IllegalArgumentException("invalid country"));

        mockMvc.perform(post("/api/v1/admin/war-geographies")
                .contentType("application/json")
                .content("{\"countryCode\":\"USA\",\"active\":true}"))
                .andExpect(status().isBadRequest());

        String oversizedRegion = "a".repeat(101);
        mockMvc.perform(post("/api/v1/admin/war-geographies")
                .contentType("application/json")
                .content("{\"countryCode\":\"US\",\"regionOrCity\":\""
                        + oversizedRegion + "\",\"active\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "WAR_GEOGRAPHY_MAINTAIN")
    void duplicateGeographyReturnsConflict() throws Exception {
        when(service.create(any()))
                .thenThrow(new DuplicateWarGeographyException("duplicate"));

        mockMvc.perform(post("/api/v1/admin/war-geographies")
                .contentType("application/json")
                .content("{\"countryCode\":\"US\",\"active\":true}"))
                .andExpect(status().isConflict());
    }
}