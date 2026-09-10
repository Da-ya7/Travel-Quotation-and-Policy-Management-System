import json
import os
from typing import Any, Protocol

import httpx

from .schemas import DocumentType, validate_fields


class SemanticExtractor(Protocol):
    def extract_fields(self, ocr_text: str, document_type: DocumentType) -> dict[str, Any]:
        ...


class GroqSemanticExtractor:
    def __init__(self, api_key: str | None = None, api_url: str | None = None, model: str | None = None):
        self.api_key = api_key or os.environ.get("GROQ_API_KEY")
        self.api_url = api_url or os.environ.get("GROQ_API_URL", "https://api.groq.com/openai/v1/chat/completions")
        self.model = model or os.environ.get("GROQ_MODEL", "llama-3.3-70b-versatile")

    def extract_fields(self, ocr_text: str, document_type: DocumentType) -> dict[str, Any]:
        if not self.api_key:
            raise RuntimeError("GROQ_API_KEY is not configured")
        prompt = (
            "Classify the document as TRAVEL_TICKET, HOTEL_BOOKING, or UNKNOWN, and return JSON with "
            "documentType and extractedFields. Return null for missing or uncertain values. "
            "Use UNKNOWN when classification is not confident. Never decide quotation validity, "
            "apply quotation rules, or invent values.\n"
            f"Document type: {document_type}\nOCR text:\n{ocr_text}"
        )
        response = httpx.post(
            self.api_url,
            headers={"Authorization": f"Bearer {self.api_key}"},
            json={"model": self.model, "temperature": 0, "response_format": {"type": "json_object"},
                  "messages": [{"role": "user", "content": prompt}]},
            timeout=30,
        )
        response.raise_for_status()
        payload = response.json()
        content = payload["choices"][0]["message"]["content"]
        result = json.loads(content)
        detected_type = result.get("documentType")
        fields = result.get("extractedFields")
        if detected_type not in {"TRAVEL_TICKET", "HOTEL_BOOKING", "UNKNOWN"} or not isinstance(fields, dict):
            raise ValueError("Malformed Groq extraction response")
        return {
            "documentType": detected_type,
            "extractedFields": validate_fields(detected_type, fields).model_dump(mode="json"),
        }
