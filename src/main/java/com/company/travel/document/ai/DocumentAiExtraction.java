package com.company.travel.document.ai;

import java.util.Map;

public record DocumentAiExtraction(String documentType, Map<String, Object> extractedFields) {
}