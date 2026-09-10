package com.company.travel.document.controller;

import com.company.travel.auth.service.CustomUserDetailsService;
import com.company.travel.auth.service.JwtService;
import com.company.travel.auth.service.UserService;
import com.company.travel.config.SecurityConfig;
import com.company.travel.document.dto.DocumentResponse;
import com.company.travel.document.exception.InvalidDocumentsException;
import com.company.travel.document.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@Import(SecurityConfig.class)
class DocumentControllerAuthorizationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserService userService;

    @Test
    void missingJwtIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/quotations/7/documents/validate"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "QUOTATION_VIEW_OWN")
    void missingDocumentAuthorityIsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/quotations/7/documents/validate"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "ravi", authorities = "DOCUMENT_UPLOAD")
    void uploadReturnsUploadedDocument() throws Exception {
        when(documentService.upload(any(), any(), anyString(), anyString()))
                .thenReturn(new DocumentResponse(11L, 7L, "TRAVEL_TICKET", "ticket.png", "image/png",
                        "UPLOADED", null, null, null));
        mockMvc.perform(multipart("/api/v1/quotations/7/documents")
                .file("document", new byte[] { 1 })
                .param("documentType", "TRAVEL_TICKET"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("UPLOADED"));
    }

    @Test
    @WithMockUser(username = "ravi", authorities = "DOCUMENT_UPLOAD")
    void invalidDocumentsReturn422() throws Exception {
        when(documentService.validate(7L, "ravi"))
                .thenThrow(new InvalidDocumentsException(List.of("document 11: DOC_COUNTRY_MISMATCH")));
        mockMvc.perform(post("/api/v1/quotations/7/documents/validate"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("INVALID_DOCUMENTS_UPLOADED"));
    }
}