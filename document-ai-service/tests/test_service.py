from datetime import date

from fastapi.testclient import TestClient

from app.main import DocumentExtractionService
from app.main import app
from app.schemas import ExtractionResponse, HotelBookingFields, TravelTicketFields, UnknownFields


class StubOcr:
    def extract_text(self, content, content_type):
        return "deterministic OCR"


class StubExtractor:
    def __init__(self, fields, detected_type=None):
        self.fields = fields
        self.detected_type = detected_type
        self.received_text = None

    def extract_fields(self, ocr_text, document_type):
        self.received_text = (ocr_text, document_type)
        return {"documentType": self.detected_type or document_type, "extractedFields": self.fields}


def test_travel_extraction_uses_mocked_layers_and_returns_structured_fields():
    extractor = StubExtractor({"passenger": "Ravi Kumar", "origin": "Colombo",
                               "destinationCountry": "FR", "destinationCityOrAirport": "Paris",
                               "departureDate": "2026-09-10", "returnOrArrivalDate": "2026-09-20"})
    result = DocumentExtractionService(StubOcr(), extractor).extract(b"data", "image/png", "TRAVEL_TICKET")
    assert result.extractedFields == TravelTicketFields(passenger="Ravi Kumar", origin="Colombo",
                                                         destinationCountry="FR", destinationCityOrAirport="Paris",
                                                         departureDate=date(2026, 9, 10),
                                                         returnOrArrivalDate=date(2026, 9, 20))
    assert extractor.received_text == ("deterministic OCR", "TRAVEL_TICKET")


def test_missing_fields_are_null():
    result = DocumentExtractionService(StubOcr(), StubExtractor({})).extract(b"data", "image/png", "HOTEL_BOOKING")
    assert result.extractedFields == HotelBookingFields()


def test_malformed_extraction_is_rejected():
    class MalformedExtractor:
        def extract_fields(self, ocr_text, document_type):
            return {"documentType": "TRAVEL_TICKET", "extractedFields": {"unexpected": "value"}}

    try:
        DocumentExtractionService(StubOcr(), MalformedExtractor()).extract(b"data", "image/png", "HOTEL_BOOKING")
        assert False, "malformed extraction should fail"
    except ValueError:
        pass


def test_unknown_document_type_returns_empty_fields():
    result = DocumentExtractionService(StubOcr(), StubExtractor({}, "UNKNOWN")).extract(
        b"data", "image/png", "TRAVEL_TICKET")
    assert result.documentType == "UNKNOWN"
    assert result.extractedFields == UnknownFields()


def test_internal_key_accepts_valid_key(monkeypatch):
    monkeypatch.setenv("DOCUMENT_AI_INTERNAL_SERVICE_KEY", "test-key")
    class StubService:
        def extract(self, content, content_type, document_type):
            return ExtractionResponse(documentType=document_type, extractedFields=TravelTicketFields(),
                                      extraction={"ocrProvider": "stub", "semanticProvider": "stub"})

    monkeypatch.setattr("app.main.service", StubService())
    response = TestClient(app).post("/internal/v1/extract",
                                    headers={"X-Internal-Service-Key": "test-key"},
                                    files={"document": ("test.png", b"data", "image/png")},
                                    data={"documentType": "TRAVEL_TICKET"})
    assert response.status_code == 200


def test_internal_key_rejects_missing_key(monkeypatch):
    monkeypatch.setenv("DOCUMENT_AI_INTERNAL_SERVICE_KEY", "test-key")
    response = TestClient(app).post("/internal/v1/extract",
                                    files={"document": ("test.png", b"data", "image/png")},
                                    data={"documentType": "TRAVEL_TICKET"})
    assert response.status_code == 403


def test_internal_key_rejects_incorrect_key(monkeypatch):
    monkeypatch.setenv("DOCUMENT_AI_INTERNAL_SERVICE_KEY", "test-key")
    response = TestClient(app).post("/internal/v1/extract",
                                    headers={"X-Internal-Service-Key": "wrong-key"},
                                    files={"document": ("test.png", b"data", "image/png")},
                                    data={"documentType": "TRAVEL_TICKET"})
    assert response.status_code == 403
