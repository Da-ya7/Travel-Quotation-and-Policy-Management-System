package com.company.travel.payment.controller;

import com.company.travel.auth.service.CustomUserDetailsService;
import com.company.travel.auth.service.JwtService;
import com.company.travel.auth.service.UserService;
import com.company.travel.config.SecurityConfig;
import com.company.travel.payment.dto.PaymentResponse;
import com.company.travel.payment.entity.Payment;
import com.company.travel.payment.service.PaymentService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import(SecurityConfig.class)
class PaymentControllerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserService userService;

    @Test
    void missingJwtIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/quotations/42/payment/success"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "uw.ravi", authorities = "QUOTATION_VIEW_OWN")
    void missingPaymentAuthorityIsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/quotations/42/payment/success"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "uw.ravi", authorities = "PAYMENT_COLLECT")
    void successWithPaymentAuthorityReturnsSavedPayment() throws Exception {
        when(paymentService.record(eq(42L), eq(Payment.Outcome.SUCCESS), eq("uw.ravi")))
                .thenReturn(response(Payment.Outcome.SUCCESS));

        mockMvc.perform(post("/api/v1/quotations/42/payment/success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quotationId").value(42))
                .andExpect(jsonPath("$.outcome").value("SUCCESS"));
    }

    @Test
    @WithMockUser(username = "uw.ravi", authorities = "PAYMENT_COLLECT")
    void failedWithPaymentAuthorityReturnsSavedPayment() throws Exception {
        when(paymentService.record(eq(42L), eq(Payment.Outcome.FAILED), eq("uw.ravi")))
                .thenReturn(response(Payment.Outcome.FAILED));

        mockMvc.perform(post("/api/v1/quotations/42/payment/failed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("FAILED"));
    }

    private PaymentResponse response(Payment.Outcome outcome) {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setQuotationId(42L);
        payment.setOutcome(outcome);
        payment.setRecordedAt(java.time.LocalDateTime.now());
        payment.setUpdatedAt(payment.getRecordedAt());
        return PaymentResponse.from(payment);
    }
}
