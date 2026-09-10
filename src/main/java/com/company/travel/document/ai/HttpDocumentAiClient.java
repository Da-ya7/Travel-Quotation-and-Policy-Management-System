package com.company.travel.document.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class HttpDocumentAiClient implements DocumentAiClient {

    private final RestClient client;

    public HttpDocumentAiClient(
            @Value("${document.ai.base-url:http://localhost:8000}") String baseUrl,
            @Value("${document.ai.internal-service-key:}") String serviceKey) {
        this.client = RestClient.builder().baseUrl(baseUrl)
                .defaultHeader("X-Internal-Service-Key", serviceKey).build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public DocumentAiExtraction extract(byte[] content, String fileName, String contentType,
            String documentType) {
        ByteArrayResource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("document", resource);
        body.add("documentType", documentType);
        Map<String, Object> response = client.post().uri("/internal/v1/extract")
                .contentType(MediaType.MULTIPART_FORM_DATA).body(body)
                .retrieve().body(Map.class);
        if (response == null || !(response.get("documentType") instanceof String)
                || !(response.get("extractedFields") instanceof Map)) {
            throw new IllegalArgumentException("Malformed document AI response");
        }
        String detectedType = (String) response.get("documentType");
        Map<String, Object> fields = (Map<String, Object>) response.get("extractedFields");
        ExtractionContractValidator.validate(fields, detectedType);
        return new DocumentAiExtraction(detectedType, fields);
    }
}