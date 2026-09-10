package com.company.travel.document.ai;

import java.util.Map;

public interface DocumentAiClient {
    DocumentAiExtraction extract(byte[] content, String fileName, String contentType,
            String documentType);
}